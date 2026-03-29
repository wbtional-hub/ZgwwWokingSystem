package com.example.lecturesystem.modules.knowledge.service.impl;

import com.example.lecturesystem.modules.aipermission.service.AiPermissionService;
import com.example.lecturesystem.modules.auth.security.LoginUser;
import com.example.lecturesystem.modules.knowledge.dto.ImportKnowledgeDocxRequest;
import com.example.lecturesystem.modules.knowledge.dto.KnowledgeBaseQueryRequest;
import com.example.lecturesystem.modules.knowledge.dto.KnowledgeChunkQueryRequest;
import com.example.lecturesystem.modules.knowledge.dto.KnowledgeDocumentQueryRequest;
import com.example.lecturesystem.modules.knowledge.dto.KnowledgeSearchRequest;
import com.example.lecturesystem.modules.knowledge.dto.SaveKnowledgeBaseRequest;
import com.example.lecturesystem.modules.knowledge.dto.ToggleKnowledgeBaseStatusRequest;
import com.example.lecturesystem.modules.knowledge.entity.DocumentImportJobEntity;
import com.example.lecturesystem.modules.knowledge.entity.KnowledgeBaseEntity;
import com.example.lecturesystem.modules.knowledge.entity.KnowledgeChunkEntity;
import com.example.lecturesystem.modules.knowledge.entity.KnowledgeDocumentEntity;
import com.example.lecturesystem.modules.knowledge.mapper.DocumentImportJobMapper;
import com.example.lecturesystem.modules.knowledge.mapper.KnowledgeBaseMapper;
import com.example.lecturesystem.modules.knowledge.mapper.KnowledgeChunkMapper;
import com.example.lecturesystem.modules.knowledge.mapper.KnowledgeDocumentMapper;
import com.example.lecturesystem.modules.knowledge.service.KnowledgeService;
import com.example.lecturesystem.modules.knowledge.support.DocxPolicyParser;
import com.example.lecturesystem.modules.knowledge.support.KnowledgeChunkBuilder;
import com.example.lecturesystem.modules.knowledge.support.ParsedDocResult;
import com.example.lecturesystem.modules.knowledge.vo.KnowledgeImportResultVO;
import com.example.lecturesystem.modules.operationlog.service.OperationLogService;
import com.example.lecturesystem.modules.permission.service.PermissionService;
import com.example.lecturesystem.modules.permission.support.CurrentUserFacade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class KnowledgeServiceImpl implements KnowledgeService {
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final KnowledgeDocumentMapper knowledgeDocumentMapper;
    private final KnowledgeChunkMapper knowledgeChunkMapper;
    private final DocumentImportJobMapper documentImportJobMapper;
    private final CurrentUserFacade currentUserFacade;
    private final PermissionService permissionService;
    private final AiPermissionService aiPermissionService;
    private final OperationLogService operationLogService;
    private final DocxPolicyParser docxPolicyParser;
    private final KnowledgeChunkBuilder knowledgeChunkBuilder;

    public KnowledgeServiceImpl(KnowledgeBaseMapper knowledgeBaseMapper,
                                KnowledgeDocumentMapper knowledgeDocumentMapper,
                                KnowledgeChunkMapper knowledgeChunkMapper,
                                DocumentImportJobMapper documentImportJobMapper,
                                CurrentUserFacade currentUserFacade,
                                PermissionService permissionService,
                                AiPermissionService aiPermissionService,
                                OperationLogService operationLogService,
                                DocxPolicyParser docxPolicyParser,
                                KnowledgeChunkBuilder knowledgeChunkBuilder) {
        this.knowledgeBaseMapper = knowledgeBaseMapper;
        this.knowledgeDocumentMapper = knowledgeDocumentMapper;
        this.knowledgeChunkMapper = knowledgeChunkMapper;
        this.documentImportJobMapper = documentImportJobMapper;
        this.currentUserFacade = currentUserFacade;
        this.permissionService = permissionService;
        this.aiPermissionService = aiPermissionService;
        this.operationLogService = operationLogService;
        this.docxPolicyParser = docxPolicyParser;
        this.knowledgeChunkBuilder = knowledgeChunkBuilder;
    }

    @Override
    public Object listBases(KnowledgeBaseQueryRequest request) {
        LoginUser loginUser = currentUserFacade.currentLoginUser();
        if (isAdmin(loginUser)) {
            return knowledgeBaseMapper.queryList(request == null ? new KnowledgeBaseQueryRequest() : request);
        }
        return knowledgeBaseMapper.queryListByUserId(loginUser.getUserId(), request == null ? new KnowledgeBaseQueryRequest() : request);
    }

    @Override
    @Transactional
    public Long saveBase(SaveKnowledgeBaseRequest request) {
        LoginUser loginUser = currentUserFacade.currentLoginUser();
        requireAdmin();
        LocalDateTime now = LocalDateTime.now();
        String baseCode = normalizeRequired(request.getBaseCode(), "知识库编码不能为空");
        String baseName = normalizeRequired(request.getBaseName(), "知识库名称不能为空");

        if (request.getId() == null) {
            if (knowledgeBaseMapper.findByCode(baseCode) != null) {
                throw new IllegalArgumentException("知识库编码已存在");
            }
            KnowledgeBaseEntity entity = new KnowledgeBaseEntity();
            entity.setBaseCode(baseCode);
            entity.setBaseName(baseName);
            entity.setDomainType(normalize(request.getDomainType()));
            entity.setDescription(normalize(request.getDescription()));
            entity.setStatus(request.getStatus());
            entity.setOwnerUserId(loginUser.getUserId());
            entity.setCreateTime(now);
            entity.setUpdateTime(now);
            entity.setCreateUser(loginUser.getUsername());
            entity.setUpdateUser(loginUser.getUsername());
            entity.setIsDeleted(Boolean.FALSE);
            knowledgeBaseMapper.insert(entity);
            operationLogService.log("KNOWLEDGE_BASE", "CREATE", entity.getId(), "新增知识库：" + baseName);
            return entity.getId();
        }

        KnowledgeBaseEntity existed = requireBase(request.getId());
        KnowledgeBaseEntity sameCode = knowledgeBaseMapper.findByCode(baseCode);
        if (sameCode != null && !sameCode.getId().equals(existed.getId())) {
            throw new IllegalArgumentException("知识库编码已存在");
        }
        existed.setBaseCode(baseCode);
        existed.setBaseName(baseName);
        existed.setDomainType(normalize(request.getDomainType()));
        existed.setDescription(normalize(request.getDescription()));
        existed.setStatus(request.getStatus());
        existed.setUpdateTime(now);
        existed.setUpdateUser(loginUser.getUsername());
        knowledgeBaseMapper.update(existed);
        operationLogService.log("KNOWLEDGE_BASE", "UPDATE", existed.getId(), "编辑知识库：" + baseName);
        return existed.getId();
    }

    @Override
    @Transactional
    public void toggleBaseStatus(ToggleKnowledgeBaseStatusRequest request) {
        LoginUser loginUser = currentUserFacade.currentLoginUser();
        requireAdmin();
        KnowledgeBaseEntity entity = requireBase(request.getId());
        knowledgeBaseMapper.updateStatus(entity.getId(), request.getStatus(), loginUser.getUsername(), LocalDateTime.now());
        operationLogService.log("KNOWLEDGE_BASE", "TOGGLE_STATUS", entity.getId(), "知识库状态变更");
    }

    @Override
    public Object listDocuments(KnowledgeDocumentQueryRequest request) {
        LoginUser loginUser = currentUserFacade.currentLoginUser();
        KnowledgeDocumentQueryRequest safeRequest = request == null ? new KnowledgeDocumentQueryRequest() : request;
        if (isAdmin(loginUser)) {
            return knowledgeDocumentMapper.queryList(safeRequest);
        }
        if (safeRequest.getBaseId() == null) {
            throw new IllegalArgumentException("非管理员查询文档时必须指定知识库");
        }
        requireKnowledgeView(loginUser.getUserId(), safeRequest.getBaseId());
        return knowledgeDocumentMapper.queryList(safeRequest);
    }

    @Override
    public Object getDocumentDetail(Long id) {
        LoginUser loginUser = currentUserFacade.currentLoginUser();
        KnowledgeDocumentEntity document = requireDocument(id);
        if (!isAdmin(loginUser)) {
            requireKnowledgeView(loginUser.getUserId(), document.getBaseId());
        }
        Object detail = knowledgeDocumentMapper.queryDetail(id);
        if (detail == null) {
            throw new IllegalArgumentException("文档不存在");
        }
        return detail;
    }

    @Override
    public Object listChunks(KnowledgeChunkQueryRequest request) {
        LoginUser loginUser = currentUserFacade.currentLoginUser();
        KnowledgeDocumentEntity document = requireDocument(request.getDocumentId());
        if (!isAdmin(loginUser)) {
            requireKnowledgeView(loginUser.getUserId(), document.getBaseId());
        }
        return knowledgeChunkMapper.queryByDocumentId(request.getDocumentId());
    }

    @Override
    @Transactional
    public KnowledgeImportResultVO importDocx(ImportKnowledgeDocxRequest request, MultipartFile file) {
        LoginUser loginUser = currentUserFacade.currentLoginUser();
        if (request == null || request.getBaseId() == null) {
            throw new IllegalArgumentException("知识库ID不能为空");
        }
        if (!isAdmin(loginUser)) {
            requireKnowledgeUpload(loginUser.getUserId(), request.getBaseId());
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请上传Word文档");
        }
        String fileName = normalizeRequired(file.getOriginalFilename(), "文件名不能为空");
        if (!fileName.toLowerCase().endsWith(".docx")) {
            throw new IllegalArgumentException("当前仅支持导入docx文档");
        }

        KnowledgeBaseEntity base = requireBase(request.getBaseId());
        DocumentImportJobEntity jobEntity = new DocumentImportJobEntity();
        jobEntity.setBaseId(base.getId());
        jobEntity.setCategoryId(request.getCategoryId());
        jobEntity.setFileName(fileName);
        jobEntity.setJobStatus("PROCESSING");
        jobEntity.setTotalChunks(0);
        jobEntity.setSuccessChunks(0);
        jobEntity.setStartTime(LocalDateTime.now());
        jobEntity.setCreateTime(LocalDateTime.now());
        jobEntity.setCreateUser(loginUser.getUsername());
        documentImportJobMapper.insert(jobEntity);

        KnowledgeImportResultVO result = new KnowledgeImportResultVO();
        result.setJobId(jobEntity.getId());
        result.setFileName(fileName);
        try {
            Path savedPath = saveFile(base.getBaseCode(), fileName, file.getBytes());
            ParsedDocResult parsedDocResult = docxPolicyParser.parse(file.getInputStream());
            KnowledgeDocumentEntity documentEntity = new KnowledgeDocumentEntity();
            documentEntity.setBaseId(base.getId());
            documentEntity.setCategoryId(request.getCategoryId());
            documentEntity.setDocTitle(normalize(parsedDocResult.getTitle()) != null ? parsedDocResult.getTitle() : fileName);
            documentEntity.setSourceType("UPLOAD");
            documentEntity.setFileName(fileName);
            documentEntity.setFilePath(savedPath.toString());
            documentEntity.setDocType("POLICY");
            documentEntity.setPolicyRegion(normalize(request.getPolicyRegion()));
            documentEntity.setPolicyLevel(normalize(request.getPolicyLevel()));
            documentEntity.setEffectiveDate(request.getEffectiveDate());
            documentEntity.setExpireDate(request.getExpireDate());
            documentEntity.setKeywords(normalize(request.getKeywords()));
            documentEntity.setSummary(normalize(request.getSummary()) != null ? request.getSummary().trim() : parsedDocResult.getSummary());
            documentEntity.setParseStatus("SUCCESS");
            documentEntity.setStatus(1);
            documentEntity.setCreateTime(LocalDateTime.now());
            documentEntity.setUpdateTime(LocalDateTime.now());
            documentEntity.setCreateUser(loginUser.getUsername());
            documentEntity.setUpdateUser(loginUser.getUsername());
            documentEntity.setIsDeleted(Boolean.FALSE);
            knowledgeDocumentMapper.insert(documentEntity);

            List<KnowledgeChunkEntity> chunks = knowledgeChunkBuilder.build(
                    documentEntity.getId(),
                    documentEntity.getBaseId(),
                    documentEntity.getCategoryId(),
                    normalize(request.getKeywords()),
                    parsedDocResult.getSections()
            );
            if (chunks.isEmpty()) {
                throw new IllegalArgumentException("文档未解析出可入库内容");
            }
            knowledgeChunkMapper.batchInsert(chunks);

            jobEntity.setJobStatus("SUCCESS");
            jobEntity.setTotalChunks(chunks.size());
            jobEntity.setSuccessChunks(chunks.size());
            jobEntity.setFinishTime(LocalDateTime.now());
            documentImportJobMapper.updateResult(jobEntity);

            operationLogService.log("KNOWLEDGE_DOCUMENT", "IMPORT_DOCX", documentEntity.getId(), "导入知识文档：" + documentEntity.getDocTitle());

            result.setDocumentId(documentEntity.getId());
            result.setJobStatus(jobEntity.getJobStatus());
            result.setTotalChunks(chunks.size());
            result.setSuccessChunks(chunks.size());
            return result;
        } catch (Exception ex) {
            jobEntity.setJobStatus("FAILED");
            jobEntity.setErrorMessage(ex.getMessage());
            jobEntity.setFinishTime(LocalDateTime.now());
            documentImportJobMapper.updateResult(jobEntity);
            result.setJobStatus("FAILED");
            result.setErrorMessage(ex.getMessage());
            throw new IllegalArgumentException(ex.getMessage(), ex);
        }
    }

    @Override
    public Object search(KnowledgeSearchRequest request) {
        LoginUser loginUser = currentUserFacade.currentLoginUser();
        requireBase(request.getBaseId());
        if (!isAdmin(loginUser)) {
            requireKnowledgeAnalyze(loginUser.getUserId(), request.getBaseId());
        }
        int topN = request.getTopN() == null || request.getTopN() <= 0 ? 10 : Math.min(request.getTopN(), 50);
        request.setTopN(topN);
        return knowledgeChunkMapper.search(request);
    }

    private KnowledgeBaseEntity requireBase(Long id) {
        KnowledgeBaseEntity entity = knowledgeBaseMapper.findById(id);
        if (entity == null) {
            throw new IllegalArgumentException("知识库不存在");
        }
        return entity;
    }

    private KnowledgeDocumentEntity requireDocument(Long id) {
        KnowledgeDocumentEntity entity = knowledgeDocumentMapper.findById(id);
        if (entity == null) {
            throw new IllegalArgumentException("文档不存在");
        }
        return entity;
    }

    private void requireAdmin() {
        LoginUser loginUser = currentUserFacade.currentLoginUser();
        if (!permissionService.isSuperAdmin(loginUser.getUserId())) {
            throw new IllegalArgumentException("仅管理员可执行该操作");
        }
    }

    private boolean isAdmin(LoginUser loginUser) {
        return permissionService.isSuperAdmin(loginUser.getUserId());
    }

    private void requireKnowledgeView(Long userId, Long baseId) {
        if (!aiPermissionService.canViewKnowledgeBase(userId, baseId)) {
            throw new IllegalArgumentException("当前用户未授权查看该知识库");
        }
    }

    private void requireKnowledgeUpload(Long userId, Long baseId) {
        if (!aiPermissionService.canUploadKnowledgeBase(userId, baseId)) {
            throw new IllegalArgumentException("当前用户未授权上传该知识库文档");
        }
    }

    private void requireKnowledgeAnalyze(Long userId, Long baseId) {
        if (!aiPermissionService.canAnalyzeKnowledgeBase(userId, baseId)) {
            throw new IllegalArgumentException("当前用户未授权分析该知识库");
        }
    }

    private String normalize(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeRequired(String text, String message) {
        String value = normalize(text);
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private Path saveFile(String baseCode, String originalFileName, byte[] bytes) throws IOException {
        LocalDate today = LocalDate.now();
        Path dir = Paths.get(System.getProperty("user.dir"), "storage", "knowledge",
                baseCode == null ? "default" : baseCode.toLowerCase(),
                today.format(DateTimeFormatter.BASIC_ISO_DATE));
        Files.createDirectories(dir);
        String safeFileName = System.currentTimeMillis() + "_" + originalFileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        Path path = dir.resolve(safeFileName);
        Files.write(path, bytes);
        return path;
    }
}