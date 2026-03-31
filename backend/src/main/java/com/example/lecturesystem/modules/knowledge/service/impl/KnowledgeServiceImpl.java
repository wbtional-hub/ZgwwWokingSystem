package com.example.lecturesystem.modules.knowledge.service.impl;

import com.example.lecturesystem.modules.aipermission.service.AiPermissionService;
import com.example.lecturesystem.modules.auth.security.LoginUser;
import com.example.lecturesystem.modules.knowledge.dto.ImportKnowledgeDocxRequest;
import com.example.lecturesystem.modules.knowledge.dto.KnowledgeBaseQueryRequest;
import com.example.lecturesystem.modules.knowledge.dto.KnowledgeChunkQueryRequest;
import com.example.lecturesystem.modules.knowledge.dto.KnowledgeDocumentQueryRequest;
import com.example.lecturesystem.modules.knowledge.dto.KnowledgeSearchRequest;
import com.example.lecturesystem.modules.knowledge.dto.SaveKnowledgeBaseRequest;
import com.example.lecturesystem.modules.knowledge.dto.SetCurrentKnowledgeBaseRequest;
import com.example.lecturesystem.modules.knowledge.dto.ToggleKnowledgeBaseStatusRequest;
import com.example.lecturesystem.modules.knowledge.dto.WebKnowledgeImportRequest;
import com.example.lecturesystem.modules.knowledge.dto.WebKnowledgePreviewRequest;
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
import com.example.lecturesystem.modules.knowledge.support.ParsedDocSection;
import com.example.lecturesystem.modules.knowledge.vo.KnowledgeImportResultVO;
import com.example.lecturesystem.modules.knowledge.vo.WebKnowledgeImportResultVO;
import com.example.lecturesystem.modules.knowledge.vo.WebKnowledgePreviewVO;
import com.example.lecturesystem.modules.operationlog.service.OperationLogService;
import com.example.lecturesystem.modules.param.entity.ParamEntity;
import com.example.lecturesystem.modules.param.mapper.ParamMapper;
import com.example.lecturesystem.modules.permission.service.PermissionService;
import com.example.lecturesystem.modules.permission.support.CurrentUserFacade;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.net.ssl.SSLException;
import java.net.ConnectException;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class KnowledgeServiceImpl implements KnowledgeService {
    private static final String CURRENT_BASE_PARAM_CODE = "AI_CURRENT_KNOWLEDGE_BASE_ID";
    private static final String CURRENT_BASE_PARAM_NAME = "当前工作知识库";
    private static final DateTimeFormatter FEEDBACK_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Pattern PUBLISH_TIME_PATTERN = Pattern.compile("(20\\d{2}[-/.年]\\d{1,2}[-/.月]\\d{1,2}(?:日)?(?:\\s+\\d{1,2}:\\d{2}(?::\\d{2})?)?)");
    private static final Pattern SCRIPT_ASYNC_PATTERN = Pattern.compile("fetch\\(|axios|XMLHttpRequest|\\$.ajax|rowId|detailId|contentId|articleId|newsId", Pattern.CASE_INSENSITIVE);
    private static final List<String> NOISE_LINE_KEYWORDS = List.of(
            "首页", "返回上页", "政策索引", "政策计算器", "政策查询", "分享", "上一篇", "下一篇",
            "面包屑", "当前位置", "打印", "关闭", "扫一扫", "微信", "微博", "友情链接", "网站地图",
            "联系我们", "主办单位", "版权所有", "copyright"
    );
    private static final List<String> HEADLESS_BROWSER_PATHS = List.of(
            "/mnt/c/Program Files (x86)/Microsoft/Edge/Application/msedge.exe",
            "/mnt/c/Program Files/Microsoft/Edge/Application/msedge.exe",
            "/mnt/c/Program Files/Google/Chrome/Application/chrome.exe",
            "/mnt/c/Program Files (x86)/Google/Chrome/Application/chrome.exe",
            "msedge",
            "google-chrome",
            "chromium",
            "chromium-browser"
    );
    private static final List<SelectorRule> TITLE_SELECTOR_RULES = List.of(
            new SelectorRule("h1", "h1"),
            new SelectorRule(".article h1", ".article h1"),
            new SelectorRule(".content h1", ".content h1"),
            new SelectorRule(".title", ".title"),
            new SelectorRule(".article-title", ".article-title"),
            new SelectorRule(".content-title", ".content-title"),
            new SelectorRule(".news-title", ".news-title"),
            new SelectorRule(".detail-title", ".detail-title"),
            new SelectorRule(".view-title", ".view-title")
    );
    private static final List<SelectorRule> BODY_SELECTOR_RULES = List.of(
            new SelectorRule("article", "article"),
            new SelectorRule(".article", ".article"),
            new SelectorRule(".article-content", ".article-content"),
            new SelectorRule(".article-detail", ".article-detail"),
            new SelectorRule(".article-body", ".article-body"),
            new SelectorRule(".content", ".content"),
            new SelectorRule(".content-box", ".content-box"),
            new SelectorRule(".content-text", ".content-text"),
            new SelectorRule(".detail-content", ".detail-content"),
            new SelectorRule(".detail-text", ".detail-text"),
            new SelectorRule(".news-content", ".news-content"),
            new SelectorRule(".view-content", ".view-content"),
            new SelectorRule(".post-content", ".post-content"),
            new SelectorRule(".main-content", ".main-content"),
            new SelectorRule(".txt", ".txt"),
            new SelectorRule(".text", ".text"),
            new SelectorRule(".TRS_Editor", ".TRS_Editor"),
            new SelectorRule(".wp_articlecontent", ".wp_articlecontent"),
            new SelectorRule(".zoom", ".zoom"),
            new SelectorRule("#content", "#content"),
            new SelectorRule("#article-content", "#article-content"),
            new SelectorRule("#zoom", "#zoom")
    );

    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final KnowledgeDocumentMapper knowledgeDocumentMapper;
    private final KnowledgeChunkMapper knowledgeChunkMapper;
    private final DocumentImportJobMapper documentImportJobMapper;
    private final CurrentUserFacade currentUserFacade;
    private final PermissionService permissionService;
    private final AiPermissionService aiPermissionService;
    private final OperationLogService operationLogService;
    private final ParamMapper paramMapper;
    private final DocxPolicyParser docxPolicyParser;
    private final KnowledgeChunkBuilder knowledgeChunkBuilder;
    private final HttpClient httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();

    public KnowledgeServiceImpl(KnowledgeBaseMapper knowledgeBaseMapper,
                                KnowledgeDocumentMapper knowledgeDocumentMapper,
                                KnowledgeChunkMapper knowledgeChunkMapper,
                                DocumentImportJobMapper documentImportJobMapper,
                                CurrentUserFacade currentUserFacade,
                                PermissionService permissionService,
                                AiPermissionService aiPermissionService,
                                OperationLogService operationLogService,
                                ParamMapper paramMapper,
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
        this.paramMapper = paramMapper;
        this.docxPolicyParser = docxPolicyParser;
        this.knowledgeChunkBuilder = knowledgeChunkBuilder;
    }

    @Override
    public Object listBases(KnowledgeBaseQueryRequest request) {
        LoginUser loginUser = currentUserFacade.currentLoginUser();
        List<com.example.lecturesystem.modules.knowledge.vo.KnowledgeBaseListItemVO> baseList;
        if (isAdmin(loginUser)) {
            baseList = knowledgeBaseMapper.queryList(request == null ? new KnowledgeBaseQueryRequest() : request);
        } else {
            baseList = knowledgeBaseMapper.queryListByUserId(loginUser.getUserId(), request == null ? new KnowledgeBaseQueryRequest() : request);
        }
        Long currentBaseId = queryCurrentBaseId();
        List<com.example.lecturesystem.modules.knowledge.vo.KnowledgeBaseListItemVO> result = new ArrayList<>(baseList);
        for (com.example.lecturesystem.modules.knowledge.vo.KnowledgeBaseListItemVO item : result) {
            item.setCurrent(Objects.equals(item.getId(), currentBaseId));
        }
        return result;
    }

    @Override
    @Transactional
    public Long saveBase(SaveKnowledgeBaseRequest request) {
        LoginUser loginUser = currentUserFacade.currentLoginUser();
        requireAdmin();
        LocalDateTime now = LocalDateTime.now();
        String baseName = normalizeRequired(request.getBaseName(), "知识库名称不能为空");
        String domainType = normalizeRequired(request.getDomainType(), "适用领域不能为空");
        String description = normalizeRequired(request.getDescription(), "知识库简介不能为空");

        if (request.getId() == null) {
            String baseCode = normalize(request.getBaseCode());
            if (baseCode == null) {
                baseCode = generateBaseCode();
            } else if (knowledgeBaseMapper.findByCode(baseCode) != null) {
                throw new IllegalArgumentException("系统生成的知识库编码重复，请稍后重试");
            }
            if (knowledgeBaseMapper.findByCode(baseCode) != null) {
                throw new IllegalArgumentException("知识库编码已存在");
            }
            KnowledgeBaseEntity entity = new KnowledgeBaseEntity();
            entity.setBaseCode(baseCode);
            entity.setBaseName(baseName);
            entity.setDomainType(domainType);
            entity.setDescription(description);
            entity.setRemark(normalize(request.getRemark()));
            entity.setStatus(request.getStatus());
            entity.setOwnerUserId(loginUser.getUserId());
            entity.setCreateTime(now);
            entity.setUpdateTime(now);
            entity.setCreateUser(loginUser.getUsername());
            entity.setUpdateUser(loginUser.getUsername());
            entity.setIsDeleted(Boolean.FALSE);
            knowledgeBaseMapper.insert(entity);
            if (entity.getStatus() != null && entity.getStatus() == 1 && queryCurrentBaseId() == null) {
                saveCurrentBaseParam(entity.getId(), loginUser.getUsername(), now);
            }
            operationLogService.log("KNOWLEDGE_BASE", "CREATE", entity.getId(), "新增知识库：" + baseName);
            return entity.getId();
        }

        KnowledgeBaseEntity existed = requireBase(request.getId());
        String baseCode = existed.getBaseCode();
        String requestCode = normalize(request.getBaseCode());
        if (requestCode != null) {
            baseCode = requestCode;
        }
        KnowledgeBaseEntity sameCode = knowledgeBaseMapper.findByCode(baseCode);
        if (sameCode != null && !sameCode.getId().equals(existed.getId())) {
            throw new IllegalArgumentException("知识库编码已存在");
        }
        existed.setBaseCode(baseCode);
        existed.setBaseName(baseName);
        existed.setDomainType(domainType);
        existed.setDescription(description);
        existed.setRemark(normalize(request.getRemark()));
        existed.setStatus(request.getStatus());
        existed.setUpdateTime(now);
        existed.setUpdateUser(loginUser.getUsername());
        knowledgeBaseMapper.update(existed);
        if (request.getStatus() != null && request.getStatus() == 1 && queryCurrentBaseId() == null) {
            saveCurrentBaseParam(existed.getId(), loginUser.getUsername(), now);
        }
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
        if (request.getStatus() != null && request.getStatus() == 0 && Objects.equals(queryCurrentBaseId(), entity.getId())) {
            clearCurrentBaseParam(loginUser.getUsername(), LocalDateTime.now());
        }
        operationLogService.log("KNOWLEDGE_BASE", "TOGGLE_STATUS", entity.getId(), "知识库状态变更");
    }

    @Override
    @Transactional
    public void setCurrentBase(SetCurrentKnowledgeBaseRequest request) {
        LoginUser loginUser = currentUserFacade.currentLoginUser();
        requireAdmin();
        KnowledgeBaseEntity entity = requireBase(request.getId());
        if (entity.getStatus() == null || entity.getStatus() != 1) {
            throw new IllegalArgumentException("请先启用该知识库，再设为当前使用");
        }
        saveCurrentBaseParam(entity.getId(), loginUser.getUsername(), LocalDateTime.now());
        operationLogService.log("KNOWLEDGE_BASE", "SET_CURRENT", entity.getId(), "设置当前工作知识库：" + entity.getBaseName());
    }

    @Override
    @Transactional
    public void deleteBase(Long id) {
        LoginUser loginUser = currentUserFacade.currentLoginUser();
        requireAdmin();
        KnowledgeBaseEntity entity = requireBase(id);
        int documentCount = knowledgeBaseMapper.countDocumentsByBaseId(id);
        int chunkCount = knowledgeBaseMapper.countChunksByBaseId(id);
        if (documentCount > 0 || chunkCount > 0) {
            throw new IllegalArgumentException("该知识库已有文档或知识内容，建议先停用，不支持直接删除");
        }
        knowledgeBaseMapper.logicalDelete(id, loginUser.getUsername(), LocalDateTime.now());
        if (Objects.equals(queryCurrentBaseId(), id)) {
            clearCurrentBaseParam(loginUser.getUsername(), LocalDateTime.now());
        }
        operationLogService.log("KNOWLEDGE_BASE", "DELETE", id, "删除知识库：" + entity.getBaseName());
    }

    @Override
    public WebKnowledgePreviewVO previewWebContent(WebKnowledgePreviewRequest request) {
        LoginUser loginUser = currentUserFacade.currentLoginUser();
        if (!isAdmin(loginUser)) {
            requireKnowledgeUpload(loginUser.getUserId(), request.getBaseId());
        }
        KnowledgeBaseEntity base = requireBase(request.getBaseId());
        LocalDateTime now = LocalDateTime.now();
        WebFetchResult fetchResult = fetchWebPage(request.getUrl(), now, base.getBaseName());

        WebKnowledgePreviewVO vo = new WebKnowledgePreviewVO();
        vo.setTitle(fetchResult.title());
        vo.setSummary(fetchResult.summary());
        vo.setContent(fetchResult.content());
        vo.setSourceUrl(fetchResult.sourceUrl());
        vo.setFeedbackText(fetchResult.feedbackText());
        vo.setSuccess(fetchResult.success());
        return vo;
    }

    @Override
    @Transactional
    public WebKnowledgeImportResultVO importWebContent(WebKnowledgeImportRequest request) {
        LoginUser loginUser = currentUserFacade.currentLoginUser();
        if (!isAdmin(loginUser)) {
            requireKnowledgeUpload(loginUser.getUserId(), request.getBaseId());
        }
        KnowledgeBaseEntity base = requireBase(request.getBaseId());
        String title = normalizeRequired(request.getTitle(), "网页标题不能为空");
        String summary = normalizeRequired(request.getSummary(), "网页摘要不能为空");
        String content = normalizeRequired(request.getContent(), "网页正文不能为空");
        String sourceUrl = normalizeRequired(request.getUrl(), "网页地址不能为空");
        LocalDateTime now = LocalDateTime.now();

        KnowledgeDocumentEntity documentEntity = new KnowledgeDocumentEntity();
        documentEntity.setBaseId(base.getId());
        documentEntity.setCategoryId(null);
        documentEntity.setDocTitle(title);
        documentEntity.setSourceType("WEB");
        documentEntity.setSourceUrl(sourceUrl);
        documentEntity.setFileName(null);
        documentEntity.setFilePath(sourceUrl);
        documentEntity.setDocType("WEB_PAGE");
        documentEntity.setPolicyRegion(null);
        documentEntity.setPolicyLevel(null);
        documentEntity.setEffectiveDate(null);
        documentEntity.setExpireDate(null);
        documentEntity.setKeywords(null);
        documentEntity.setSummary(summary);
        documentEntity.setFetchTime(now);
        documentEntity.setParseStatus("SUCCESS");
        documentEntity.setStatus(request.getStatus());
        documentEntity.setCreateTime(now);
        documentEntity.setUpdateTime(now);
        documentEntity.setCreateUser(loginUser.getUsername());
        documentEntity.setUpdateUser(loginUser.getUsername());
        documentEntity.setIsDeleted(Boolean.FALSE);
        knowledgeDocumentMapper.insert(documentEntity);

        List<ParsedDocSection> sections = new ArrayList<>();
        ParsedDocSection bodySection = new ParsedDocSection();
        bodySection.setSectionNo(1);
        bodySection.setHeadingPath(title);
        bodySection.setChunkType("web-content");
        bodySection.setContentText(content);
        sections.add(bodySection);
        List<KnowledgeChunkEntity> chunks = knowledgeChunkBuilder.build(documentEntity.getId(), documentEntity.getBaseId(), null, null, sections);
        if (!chunks.isEmpty()) {
            knowledgeChunkMapper.batchInsert(chunks);
        }

        String feedbackText = buildImportFeedbackText(now, sourceUrl, base.getBaseName(), title, content.length(), documentEntity.getId(), null);
        operationLogService.log("KNOWLEDGE_DOCUMENT", "IMPORT_WEB", documentEntity.getId(), "网页导入知识文档：" + title);

        WebKnowledgeImportResultVO vo = new WebKnowledgeImportResultVO();
        vo.setDocumentId(documentEntity.getId());
        vo.setFeedbackText(feedbackText);
        return vo;
    }

    @Override
    public WebKnowledgePreviewVO previewSnapshotContent(Long baseId, MultipartFile file) {
        LoginUser loginUser = currentUserFacade.currentLoginUser();
        if (baseId == null) {
            throw new IllegalArgumentException("所属知识库不能为空");
        }
        if (!isAdmin(loginUser)) {
            requireKnowledgeUpload(loginUser.getUserId(), baseId);
        }
        KnowledgeBaseEntity base = requireBase(baseId);
        SnapshotPreviewResult previewResult = parseSnapshotPreview(file, base.getBaseName(), LocalDateTime.now());
        WebKnowledgePreviewVO vo = new WebKnowledgePreviewVO();
        vo.setTitle(previewResult.title());
        vo.setSummary(previewResult.summary());
        vo.setContent(previewResult.content());
        vo.setSourceUrl(previewResult.sourceUrl());
        vo.setFileName(previewResult.fileName());
        vo.setFeedbackText(previewResult.feedbackText());
        vo.setSuccess(previewResult.success());
        return vo;
    }

    @Override
    @Transactional
    public WebKnowledgeImportResultVO importSnapshotContent(Long baseId, Integer status, String title, String summary, String content,
                                                            String sourceUrl, String fileName, MultipartFile file) {
        LoginUser loginUser = currentUserFacade.currentLoginUser();
        if (baseId == null) {
            throw new IllegalArgumentException("所属知识库不能为空");
        }
        if (!isAdmin(loginUser)) {
            requireKnowledgeUpload(loginUser.getUserId(), baseId);
        }
        KnowledgeBaseEntity base = requireBase(baseId);
        String finalTitle = normalizeRequired(title, "网页快照标题不能为空");
        String finalSummary = normalizeRequired(summary, "网页快照摘要不能为空");
        String finalContent = normalizeRequired(content, "网页快照正文不能为空");
        String finalFileName = normalizeRequired(fileName != null && !fileName.isBlank() ? fileName : file == null ? null : file.getOriginalFilename(), "请选择网页快照文件");
        String fileExtension = detectSnapshotExtension(finalFileName);
        LocalDateTime now = LocalDateTime.now();

        KnowledgeDocumentEntity documentEntity = new KnowledgeDocumentEntity();
        documentEntity.setBaseId(base.getId());
        documentEntity.setCategoryId(null);
        documentEntity.setDocTitle(finalTitle);
        documentEntity.setSourceType("WEB_SNAPSHOT");
        documentEntity.setSourceUrl(normalize(sourceUrl));
        documentEntity.setFileName(finalFileName);
        documentEntity.setFilePath(saveSnapshotFile(base, file, finalFileName));
        documentEntity.setDocType("WEB_SNAPSHOT_" + fileExtension.toUpperCase());
        documentEntity.setPolicyRegion(null);
        documentEntity.setPolicyLevel(null);
        documentEntity.setEffectiveDate(null);
        documentEntity.setExpireDate(null);
        documentEntity.setKeywords(null);
        documentEntity.setSummary(finalSummary);
        documentEntity.setFetchTime(now);
        documentEntity.setParseStatus("SUCCESS");
        documentEntity.setStatus(status == null ? 1 : status);
        documentEntity.setCreateTime(now);
        documentEntity.setUpdateTime(now);
        documentEntity.setCreateUser(loginUser.getUsername());
        documentEntity.setUpdateUser(loginUser.getUsername());
        documentEntity.setIsDeleted(Boolean.FALSE);
        knowledgeDocumentMapper.insert(documentEntity);

        List<ParsedDocSection> sections = new ArrayList<>();
        ParsedDocSection bodySection = new ParsedDocSection();
        bodySection.setSectionNo(1);
        bodySection.setHeadingPath(finalTitle);
        bodySection.setChunkType("web-snapshot");
        bodySection.setContentText(finalContent);
        sections.add(bodySection);
        List<KnowledgeChunkEntity> chunks = knowledgeChunkBuilder.build(documentEntity.getId(), documentEntity.getBaseId(), null, null, sections);
        if (!chunks.isEmpty()) {
            knowledgeChunkMapper.batchInsert(chunks);
        }

        String feedbackText = buildSnapshotImportFeedbackText(
                now,
                finalFileName,
                fileExtension,
                file == null ? 0L : file.getSize(),
                base.getBaseName(),
                finalTitle,
                finalContent.length(),
                documentEntity.getId(),
                null
        );
        operationLogService.log("KNOWLEDGE_DOCUMENT", "IMPORT_WEB_SNAPSHOT", documentEntity.getId(), "网页快照导入知识文档：" + finalTitle);

        WebKnowledgeImportResultVO vo = new WebKnowledgeImportResultVO();
        vo.setDocumentId(documentEntity.getId());
        vo.setFeedbackText(feedbackText);
        return vo;
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
            throw new IllegalArgumentException("请上传Word或PDF文档");
        }
        String fileName = normalizeRequired(file.getOriginalFilename(), "文件名不能为空");
        String lowerFileName = fileName.toLowerCase();
        if (!lowerFileName.endsWith(".docx") && !lowerFileName.endsWith(".pdf")) {
            throw new IllegalArgumentException("当前仅支持导入docx或pdf文档");
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
            ParsedDocResult parsedDocResult = lowerFileName.endsWith(".pdf")
                    ? parsePdfDocument(file, fileName)
                    : docxPolicyParser.parse(file.getInputStream());
            KnowledgeDocumentEntity documentEntity = new KnowledgeDocumentEntity();
            documentEntity.setBaseId(base.getId());
            documentEntity.setCategoryId(request.getCategoryId());
            documentEntity.setDocTitle(normalize(parsedDocResult.getTitle()) != null ? parsedDocResult.getTitle() : fileName);
            documentEntity.setSourceType("UPLOAD");
            documentEntity.setFileName(fileName);
            documentEntity.setFilePath(savedPath.toString());
            documentEntity.setDocType(lowerFileName.endsWith(".pdf") ? "PDF" : "POLICY");
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

            operationLogService.log("KNOWLEDGE_DOCUMENT", lowerFileName.endsWith(".pdf") ? "IMPORT_PDF" : "IMPORT_DOCX", documentEntity.getId(), "导入知识文档：" + documentEntity.getDocTitle());

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

    private SnapshotPreviewResult parseSnapshotPreview(MultipartFile file, String baseName, LocalDateTime now) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择网页快照文件");
        }
        String fileName = normalizeRequired(file.getOriginalFilename(), "请选择网页快照文件");
        String extension = detectSnapshotExtension(fileName);
        if (!List.of("mhtml", "html", "htm").contains(extension)) {
            throw new IllegalArgumentException("仅支持上传 .mhtml、.html、.htm 网页快照文件");
        }
        SnapshotExtractResult extractResult = extractSnapshotHtml(file, extension);
        ParsedWebContent parsed = extractResult.success()
                ? parseWebContent(Jsoup.parse(extractResult.html(), humanizeNull(extractResult.sourceUrl(), fileName)), extractResult.htmlLength())
                : new ParsedWebContent("", "", "", false, "", 0, "", "", "", List.of(), List.of(), humanizeNull(extractResult.failureReason(), "网页快照解析失败"));
        String feedbackText = buildSnapshotPreviewFeedbackText(now, fileName, extension, file.getSize(), baseName, extractResult, parsed);
        return new SnapshotPreviewResult(
                parsed.success(),
                parsed.title(),
                parsed.summary(),
                parsed.content(),
                humanizeNull(extractResult.sourceUrl(), fileName),
                fileName,
                feedbackText
        );
    }

    private ParsedDocResult parsePdfDocument(MultipartFile file, String fileName) throws IOException {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String rawText = stripper.getText(document);
            String cleanText = cleanPdfText(rawText);
            if (cleanText.isBlank()) {
                throw new IllegalArgumentException("PDF未解析出可入库文本内容");
            }
            ParsedDocResult result = new ParsedDocResult();
            String title = extractPdfTitle(cleanText, fileName);
            result.setTitle(title);
            result.setSummary(buildSummary(cleanText));

            ParsedDocSection section = new ParsedDocSection();
            section.setSectionNo(1);
            section.setHeadingPath(title);
            section.setChunkType("pdf-body");
            section.setContentText(cleanText);
            result.setSections(List.of(section));
            return result;
        }
    }

    private String cleanPdfText(String rawText) {
        String normalized = normalize(rawText);
        if (normalized == null) {
            return "";
        }
        return normalized
                .replace("\u00A0", " ")
                .replaceAll("\\r\\n?", "\n")
                .replaceAll("[\\t\\f]+", " ")
                .replaceAll(" {2,}", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private String extractPdfTitle(String content, String fileName) {
        String normalizedContent = normalize(content);
        if (normalizedContent != null) {
            for (String line : normalizedContent.split("\n")) {
                String candidate = cleanTitle(line);
                if (candidate != null && candidate.length() >= 6) {
                    return candidate;
                }
            }
        }
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
    }

    private SnapshotExtractResult extractSnapshotHtml(MultipartFile file, String extension) {
        try {
            byte[] bytes = file.getBytes();
            if ("html".equals(extension) || "htm".equals(extension)) {
                String html = decodeHtmlBytes(bytes, detectCharsetFromHtml(bytes));
                return new SnapshotExtractResult(true, false, true, html, null, null, "HTML 文件直接解析", null);
            }
            return extractMhtml(bytes);
        } catch (Exception ex) {
            return new SnapshotExtractResult(false, "mhtml".equals(extension), false, "", null, null, "网页快照解析失败", humanizeNull(ex.getMessage(), "文件读取失败"));
        }
    }

    private SnapshotExtractResult extractMhtml(byte[] bytes) {
        String raw = new String(bytes, StandardCharsets.ISO_8859_1);
        String boundary = matchHeaderValue(raw, "(?im)^Content-Type:.*boundary=\"?([^\";\\r\\n]+)\"?");
        if (boundary == null) {
            return new SnapshotExtractResult(false, true, false, "", null, null, "未在 MHTML 头信息中识别到 boundary", null);
        }
        String delimiter = "--" + boundary;
        String[] parts = raw.split(Pattern.quote(delimiter));
        for (String part : parts) {
            String normalizedPart = normalize(part);
            if (normalizedPart == null || normalizedPart.equals("--")) {
                continue;
            }
            int separatorIndex = part.indexOf("\r\n\r\n");
            int separatorLength = 4;
            if (separatorIndex < 0) {
                separatorIndex = part.indexOf("\n\n");
                separatorLength = 2;
            }
            if (separatorIndex < 0) {
                continue;
            }
            String headers = part.substring(0, separatorIndex);
            String body = part.substring(separatorIndex + separatorLength);
            String contentType = matchHeaderValue(headers, "(?im)^Content-Type:\\s*([^;\\r\\n]+)");
            if (contentType == null || !contentType.toLowerCase().contains("text/html")) {
                continue;
            }
            String transferEncoding = humanizeNull(matchHeaderValue(headers, "(?im)^Content-Transfer-Encoding:\\s*([^;\\r\\n]+)"), "8bit");
            String charsetName = humanizeNull(matchHeaderValue(headers, "(?im)^Content-Type:.*charset=\"?([^\";\\r\\n]+)\"?"), "UTF-8");
            String sourceUrl = matchHeaderValue(headers, "(?im)^Content-Location:\\s*(.+)$");
            byte[] bodyBytes = body.getBytes(StandardCharsets.ISO_8859_1);
            byte[] decoded = decodeMhtmlBody(bodyBytes, transferEncoding);
            String html = decodeHtmlBytes(decoded, safeCharset(charsetName));
            if (normalize(html) != null && html.toLowerCase().contains("<html")) {
                return new SnapshotExtractResult(true, true, true, html, sourceUrl, transferEncoding, "已在 MHTML MIME 结构中提取主 HTML", null);
            }
        }
        return new SnapshotExtractResult(false, true, false, "", null, null, "未在MIME结构中识别到有效HTML主体", null);
    }

    private byte[] decodeMhtmlBody(byte[] bodyBytes, String transferEncoding) {
        String normalizedEncoding = transferEncoding == null ? "" : transferEncoding.trim().toLowerCase();
        if ("base64".equals(normalizedEncoding)) {
            String compact = new String(bodyBytes, StandardCharsets.ISO_8859_1).replaceAll("\\s+", "");
            return Base64.getMimeDecoder().decode(compact);
        }
        if ("quoted-printable".equals(normalizedEncoding)) {
            return decodeQuotedPrintable(bodyBytes);
        }
        return bodyBytes;
    }

    private byte[] decodeQuotedPrintable(byte[] input) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        for (int index = 0; index < input.length; index++) {
            int value = input[index];
            if (value == '=') {
                if (index + 2 < input.length) {
                    char first = (char) input[index + 1];
                    char second = (char) input[index + 2];
                    if (first == '\r' && second == '\n') {
                        index += 2;
                        continue;
                    }
                    if (first == '\n') {
                        index += 1;
                        continue;
                    }
                    if (isHex(first) && isHex(second)) {
                        output.write(Integer.parseInt("" + first + second, 16));
                        index += 2;
                        continue;
                    }
                }
            }
            output.write(value);
        }
        return output.toByteArray();
    }

    private boolean isHex(char ch) {
        return (ch >= '0' && ch <= '9')
                || (ch >= 'A' && ch <= 'F')
                || (ch >= 'a' && ch <= 'f');
    }

    private String decodeHtmlBytes(byte[] bytes, Charset fallbackCharset) {
        Charset charset = detectCharsetFromHtml(bytes);
        if (charset == null) {
            charset = fallbackCharset == null ? StandardCharsets.UTF_8 : fallbackCharset;
        }
        return new String(bytes, charset);
    }

    private Charset detectCharsetFromHtml(byte[] bytes) {
        String probe = new String(bytes, StandardCharsets.ISO_8859_1);
        String charsetName = matchHeaderValue(probe, "(?i)charset=['\"]?([a-zA-Z0-9_\\-]+)");
        return safeCharset(charsetName);
    }

    private Charset safeCharset(String charsetName) {
        try {
            return charsetName == null ? StandardCharsets.UTF_8 : Charset.forName(charsetName.trim());
        } catch (Exception ex) {
            return StandardCharsets.UTF_8;
        }
    }

    private String matchHeaderValue(String text, String regex) {
        if (text == null) {
            return null;
        }
        Matcher matcher = Pattern.compile(regex).matcher(text);
        if (matcher.find()) {
            return normalize(matcher.group(1));
        }
        return null;
    }

    private String detectSnapshotExtension(String fileName) {
        String normalized = normalizeRequired(fileName, "请选择网页快照文件");
        int index = normalized.lastIndexOf('.');
        if (index < 0 || index == normalized.length() - 1) {
            return "";
        }
        return normalized.substring(index + 1).toLowerCase();
    }

    private String saveSnapshotFile(KnowledgeBaseEntity base, MultipartFile file, String fileName) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        try {
            Path path = saveFile(base.getBaseCode(), fileName, file.getBytes());
            return path.toString();
        } catch (IOException ex) {
            throw new IllegalArgumentException("网页快照保存失败，请稍后重试");
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

    private Long queryCurrentBaseId() {
        ParamEntity entity = paramMapper.findByCode(CURRENT_BASE_PARAM_CODE);
        if (entity == null || entity.getStatus() == null || entity.getStatus() != 1) {
            return null;
        }
        String rawValue = normalize(entity.getParamValue());
        if (rawValue == null) {
            return null;
        }
        try {
            return Long.parseLong(rawValue);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void saveCurrentBaseParam(Long baseId, String operator, LocalDateTime now) {
        ParamEntity entity = paramMapper.findByCode(CURRENT_BASE_PARAM_CODE);
        if (entity == null) {
            ParamEntity newEntity = new ParamEntity();
            newEntity.setParamCode(CURRENT_BASE_PARAM_CODE);
            newEntity.setParamName(CURRENT_BASE_PARAM_NAME);
            newEntity.setParamValue(baseId == null ? "" : String.valueOf(baseId));
            newEntity.setStatus(1);
            newEntity.setRemark("知识库管理页当前工作知识库");
            newEntity.setCreateTime(now);
            newEntity.setUpdateTime(now);
            newEntity.setCreateUser(operator);
            newEntity.setUpdateUser(operator);
            newEntity.setIsDeleted(Boolean.FALSE);
            paramMapper.insert(newEntity);
            return;
        }
        entity.setParamName(CURRENT_BASE_PARAM_NAME);
        entity.setParamValue(baseId == null ? "" : String.valueOf(baseId));
        entity.setStatus(1);
        entity.setRemark("知识库管理页当前工作知识库");
        entity.setUpdateTime(now);
        entity.setUpdateUser(operator);
        paramMapper.update(entity);
    }

    private void clearCurrentBaseParam(String operator, LocalDateTime now) {
        saveCurrentBaseParam(null, operator, now);
    }

    private String generateBaseCode() {
        String prefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("'kb_'yyyyMMddHHmmssSSS"));
        for (int index = 0; index < 8; index++) {
            String candidate = prefix + ThreadLocalRandom.current().nextInt(1000, 10000);
            if (knowledgeBaseMapper.findByCode(candidate) == null) {
                return candidate;
            }
        }
        return "kb_" + System.currentTimeMillis();
    }

    private WebFetchResult fetchWebPage(String rawUrl, LocalDateTime requestTime, String baseName) {
        String url = normalizeRequired(rawUrl, "网页地址不能为空");
        try {
            HtmlFetchResult normalFetch = fetchHtmlWithHttp(url);
            ParsedWebContent normalParsed = parseFetchedHtml(normalFetch.finalUrl(), normalFetch.html(), normalFetch.success(), normalFetch.failureType());
            FetchDiagnostics diagnostics = diagnoseFetch(normalFetch, normalParsed);
            boolean fallbackTriggered = shouldTriggerRenderFallback(normalFetch, diagnostics, normalParsed);
            RenderFetchResult renderFetch = fallbackTriggered ? fetchHtmlWithHeadlessBrowser(normalFetch.finalUrl()) : RenderFetchResult.notTriggered();
            ParsedWebContent renderParsed = renderFetch.success()
                    ? parseWebContent(Jsoup.parse(renderFetch.html(), renderFetch.finalUrl()), renderFetch.htmlLength())
                    : null;
            ModeSelection selection = chooseBestParsedResult(normalFetch.finalUrl(), normalParsed, renderFetch, renderParsed);
            String feedbackText = buildPreviewFeedbackText(
                    requestTime,
                    selection.finalUrl(),
                    baseName,
                    normalFetch.statusCode(),
                    normalParsed,
                    diagnostics,
                    fallbackTriggered,
                    renderFetch,
                    renderParsed,
                    selection.finalMode(),
                    selection.finalParsed(),
                    normalFetch
            );
            return new WebFetchResult(
                    selection.finalParsed().success(),
                    selection.finalParsed().title(),
                    selection.finalParsed().summary(),
                    selection.finalParsed().content(),
                    selection.finalUrl(),
                    feedbackText
            );
        } catch (Exception ex) {
            RequestFailureInfo failureInfo = classifyRequestFailure(ex);
            HtmlFetchResult failedFetch = HtmlFetchResult.failed(
                    "HttpClient",
                    LocalDateTime.now(),
                    url,
                    url,
                    failureInfo.failureType(),
                    ex
            );
            ParsedWebContent normalParsed = parseFetchedHtml(url, "", false, failureInfo.failureType());
            FetchDiagnostics diagnostics = diagnoseFetch(failedFetch, normalParsed);
            RenderFetchResult renderFetch = fetchHtmlWithHeadlessBrowser(url);
            ParsedWebContent renderParsed = renderFetch.success()
                    ? parseWebContent(Jsoup.parse(renderFetch.html(), renderFetch.finalUrl()), renderFetch.htmlLength())
                    : null;
            ModeSelection selection = chooseBestParsedResult(url, normalParsed, renderFetch, renderParsed);
            String feedbackText = buildPreviewFeedbackText(
                    requestTime,
                    selection.finalUrl(),
                    baseName,
                    failedFetch.statusCode(),
                    normalParsed,
                    diagnostics,
                    true,
                    renderFetch,
                    renderParsed,
                    selection.finalMode(),
                    selection.finalParsed(),
                    failedFetch
            );
            return new WebFetchResult(
                    selection.finalParsed().success(),
                    selection.finalParsed().title(),
                    selection.finalParsed().summary(),
                    selection.finalParsed().content(),
                    selection.finalUrl(),
                    feedbackText
            );
        }
    }

    private ParsedWebContent parseFetchedHtml(String url, String html, boolean requestSucceeded, String failureType) {
        if (!requestSucceeded || html == null || html.isBlank()) {
            return new ParsedWebContent(
                    "",
                    "",
                    "",
                    false,
                    "",
                    html == null ? 0 : html.length(),
                    "",
                    "",
                    "",
                    List.of(),
                    List.of(),
                    humanizeNull(failureType, "HTML为空")
            );
        }
        return parseWebContent(Jsoup.parse(html, url), html.length());
    }

    private HtmlFetchResult fetchHtmlWithHttp(String url) {
        LocalDateTime requestStartTime = LocalDateTime.now();
        URI uri = URI.create(url);
        try {
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(12))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .header("Referer", buildReferer(url))
                    .header("Cache-Control", "no-cache")
                    .header("Connection", "keep-alive")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String html = response.body() == null ? "" : response.body();
            String finalUrl = response.uri() == null ? url : response.uri().toString();
            boolean redirected = !Objects.equals(normalize(url), normalize(finalUrl));
            int redirectCount = redirected ? 1 : 0;
            String failureType = classifyHttpStatusFailure(response.statusCode(), html);
            boolean success = response.statusCode() < 400 && html != null && !html.isBlank();
            return new HtmlFetchResult(
                    success,
                    "HttpClient",
                    requestStartTime,
                    url,
                    response.statusCode(),
                    finalUrl,
                    html,
                    redirected,
                    redirectCount,
                    "",
                    "",
                    humanizeNull(failureType, success ? "" : "HTML为空"),
                    false,
                    false,
                    false,
                    false
            );
        } catch (Exception ex) {
            RequestFailureInfo failureInfo = classifyRequestFailure(ex);
            return HtmlFetchResult.failed("HttpClient", requestStartTime, url, url, failureInfo.failureType(), ex);
        }
    }

    private String buildReferer(String url) {
        try {
            URI uri = URI.create(url);
            if (uri.getScheme() == null || uri.getHost() == null) {
                return url;
            }
            return uri.getScheme() + "://" + uri.getHost() + "/";
        } catch (Exception ex) {
            return url;
        }
    }

    private FetchDiagnostics diagnoseFetch(HtmlFetchResult fetchResult, ParsedWebContent parsed) {
        String html = fetchResult.html();
        String lowerHtml = html == null ? "" : html.toLowerCase();
        boolean requestFailed = !fetchResult.success();
        boolean htmlEmpty = fetchResult.html() == null || fetchResult.html().isBlank();
        boolean shortHtml = fetchResult.htmlLength() > 0 && fetchResult.htmlLength() < 5000;
        boolean titleMissing = parsed.title() == null || parsed.title().isBlank();
        boolean contentEmpty = parsed.content() == null || parsed.content().isBlank();
        boolean contentShort = contentEmpty || parsed.content().length() < 120;
        boolean scriptAsyncDetected = SCRIPT_ASYNC_PATTERN.matcher(lowerHtml).find();
        boolean iframeDetected = lowerHtml.contains("<iframe");
        boolean detailParamDetected = lowerHtml.contains("rowid")
                || lowerHtml.contains("detailid")
                || lowerHtml.contains("contentid")
                || lowerHtml.contains("articleid")
                || lowerHtml.contains("newsid");
        boolean suspectedDynamicByRequestFailure = requestFailed && (scriptAsyncDetected || iframeDetected || detailParamDetected);
        boolean suspectedDynamic = (shortHtml && titleMissing && contentShort)
                || (titleMissing && contentShort && (scriptAsyncDetected || iframeDetected || detailParamDetected))
                || (contentShort && scriptAsyncDetected && detailParamDetected)
                || suspectedDynamicByRequestFailure;
        boolean suspectedHeaderIssue = (fetchResult.statusCode() == null || fetchResult.statusCode() == 200)
                && shortHtml
                && titleMissing
                && contentShort
                && !scriptAsyncDetected
                && !iframeDetected
                && !detailParamDetected;
        return new FetchDiagnostics(
                requestFailed,
                htmlEmpty,
                shortHtml,
                titleMissing,
                contentEmpty,
                scriptAsyncDetected,
                iframeDetected,
                detailParamDetected,
                suspectedDynamic,
                suspectedHeaderIssue,
                parsed.content() == null ? 0 : parsed.content().length(),
                humanizeNull(fetchResult.failureType(), fetchResult.exceptionMessage())
        );
    }

    private boolean shouldTriggerRenderFallback(HtmlFetchResult fetchResult,
                                                FetchDiagnostics diagnostics,
                                                ParsedWebContent parsed) {
        if (parsed.success() && (parsed.content() != null && parsed.content().length() >= 200)) {
            return false;
        }
        return diagnostics.requestFailed()
                || fetchResult.statusCode() == null
                || diagnostics.htmlEmpty()
                || diagnostics.shortHtml()
                || diagnostics.titleMissing()
                || diagnostics.contentEmpty()
                || !parsed.success()
                || diagnostics.suspectedDynamicPage()
                || diagnostics.suspectedHeaderIssue();
    }

    private RenderFetchResult fetchHtmlWithHeadlessBrowser(String url) {
        for (String browserPath : HEADLESS_BROWSER_PATHS) {
            if (!canUseBrowser(browserPath)) {
                continue;
            }
            try {
                Process process = new ProcessBuilder(
                        browserPath,
                        "--headless",
                        "--disable-gpu",
                        "--no-first-run",
                        "--no-default-browser-check",
                        "--disable-dev-shm-usage",
                        "--virtual-time-budget=8000",
                        "--dump-dom",
                        url
                ).redirectErrorStream(true).start();
                boolean finished = process.waitFor(15, TimeUnit.SECONDS);
                if (!finished) {
                    process.destroyForcibly();
                    return new RenderFetchResult(true, false, browserPath, url, "", "浏览器渲染超时");
                }
                String html = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                if (process.exitValue() == 0 && html != null && !html.isBlank()) {
                    return new RenderFetchResult(true, true, browserPath, url, html, null);
                }
                String error = normalize(html);
                return new RenderFetchResult(true, false, browserPath, url, "", humanizeNull(error, "浏览器渲染未返回有效 HTML"));
            } catch (Exception ex) {
                String error = humanizeNull(normalize(ex.getMessage()), "浏览器渲染失败");
                if (browserPath.equals(HEADLESS_BROWSER_PATHS.get(HEADLESS_BROWSER_PATHS.size() - 1))) {
                    return new RenderFetchResult(true, false, browserPath, url, "", error);
                }
            }
        }
        return new RenderFetchResult(true, false, "", url, "", "未找到可用的浏览器渲染环境");
    }

    private boolean canUseBrowser(String browserPath) {
        if (browserPath.contains("/")) {
            return Files.exists(Path.of(browserPath));
        }
        return true;
    }

    private ModeSelection chooseBestParsedResult(String defaultUrl,
                                                 ParsedWebContent normalParsed,
                                                 RenderFetchResult renderFetch,
                                                 ParsedWebContent renderParsed) {
        if (renderParsed == null) {
            return new ModeSelection("普通HTTP", defaultUrl, normalParsed);
        }
        int normalScore = scoreParsedWebContent(normalParsed);
        int renderScore = scoreParsedWebContent(renderParsed);
        if (renderParsed.success() && (!normalParsed.success() || renderScore >= normalScore + 40)) {
            return new ModeSelection("浏览器渲染", renderFetch.finalUrl(), renderParsed);
        }
        if (!normalParsed.success() && renderScore > normalScore) {
            return new ModeSelection("浏览器渲染", renderFetch.finalUrl(), renderParsed);
        }
        return new ModeSelection("普通HTTP", defaultUrl, normalParsed);
    }

    private int scoreParsedWebContent(ParsedWebContent parsed) {
        if (parsed == null) {
            return 0;
        }
        int titleScore = parsed.title() == null || parsed.title().isBlank() ? 0 : Math.min(parsed.title().length() * 2, 80);
        int contentLength = parsed.content() == null ? 0 : parsed.content().length();
        int contentScore = Math.min(contentLength / 20, 220);
        int chineseScore = Math.min(countChineseChars(parsed.content()) / 15, 160);
        int summaryScore = parsed.summary() == null || parsed.summary().isBlank() ? 0 : 20;
        int successScore = parsed.success() ? 120 : 0;
        return titleScore + contentScore + chineseScore + summaryScore + successScore;
    }

    private ParsedWebContent parseWebContent(Document document, int htmlLength) {
        Document cloned = document.clone();
        cloned.select("script,style,noscript,header,footer,nav,aside,form,button,iframe,svg,canvas").remove();
        cloned.select(".breadcrumb,.crumb,.breadcrumbs,.nav,.header,.footer,.aside,.share,.toolbar,.tools,.related,.recommend,.pagination").remove();

        String rawPageTitle = normalize(document.title());
        String publishTime = extractPublishTime(cloned);
        TitleExtractionResult titleResult = extractTitle(cloned, rawPageTitle);
        BodyExtractionResult bodyResult = extractBody(cloned, titleResult.title());

        List<TitleAttempt> titleAttempts = new ArrayList<>(titleResult.attempts());
        String finalTitle = titleResult.title();
        String finalTitleStrategy = titleResult.strategy();
        if ((finalTitle == null || finalTitle.isBlank()) && bodyResult.valid()) {
            String firstLineTitle = extractTitleFromContentFirstLine(bodyResult.content());
            if (isValidTitleCandidate(firstLineTitle)) {
                finalTitle = firstLineTitle;
                finalTitleStrategy = "正文首行兜底";
                titleAttempts.add(new TitleAttempt("正文首行兜底", "成功", firstLineTitle));
            } else {
                titleAttempts.add(new TitleAttempt("正文首行兜底", "失败", "正文首行不符合文章标题特征"));
            }
        }

        String safeTitle = finalTitle == null ? "" : finalTitle;
        String safeContent = bodyResult.content() == null ? "" : bodyResult.content();
        String summary = buildSummary(safeContent);
        boolean success = !safeTitle.isBlank() && bodyResult.valid();
        String failureReason = success ? null : humanizeNull(bodyResult.failureReason(),
                safeTitle.isBlank() ? "未提取到有效标题" : "未提取到有效正文");

        return new ParsedWebContent(
                safeTitle,
                summary,
                safeContent,
                success,
                rawPageTitle == null ? "" : rawPageTitle,
                htmlLength,
                finalTitleStrategy == null ? "" : finalTitleStrategy,
                bodyResult.strategy() == null ? "" : bodyResult.strategy(),
                publishTime == null ? "" : publishTime,
                titleAttempts,
                bodyResult.attempts(),
                failureReason
        );
    }

    private TitleExtractionResult extractTitle(Document document, String rawPageTitle) {
        List<TitleAttempt> attempts = new ArrayList<>();
        for (SelectorRule rule : TITLE_SELECTOR_RULES) {
            Elements elements = document.select(rule.selector());
            if (elements.isEmpty()) {
                attempts.add(new TitleAttempt(rule.label(), "失败", "未命中节点"));
                continue;
            }
            for (Element element : elements) {
                String candidate = cleanTitle(element.text());
                if (!isValidTitleCandidate(candidate)) {
                    attempts.add(new TitleAttempt(rule.label(), "失败", humanizeNull(candidate, "命中但疑似导航、站点名或标题过短")));
                    continue;
                }
                attempts.add(new TitleAttempt(rule.label(), "成功", candidate));
                return new TitleExtractionResult(candidate, rule.label(), attempts);
            }
        }
        String ogTitle = cleanTitle(document.select("meta[property=og:title],meta[name=og:title]").attr("content"));
        if (isValidTitleCandidate(ogTitle)) {
            attempts.add(new TitleAttempt("meta[property=\"og:title\"]", "成功", ogTitle));
            return new TitleExtractionResult(ogTitle, "meta[property=\"og:title\"]", attempts);
        }
        attempts.add(new TitleAttempt("meta[property=\"og:title\"]", "失败", humanizeNull(ogTitle, "未命中或疑似站点标题")));

        String titleTag = extractBestTitleFromPageTitle(rawPageTitle);
        if (isValidTitleCandidate(titleTag)) {
            attempts.add(new TitleAttempt("<title>", "成功", titleTag));
            return new TitleExtractionResult(titleTag, "<title>", attempts);
        }
        attempts.add(new TitleAttempt("<title>", "失败", humanizeNull(rawPageTitle, "未命中或疑似站点标题")));
        return new TitleExtractionResult("", "", attempts);
    }

    private String buildPreviewFeedbackText(LocalDateTime requestTime,
                                            String url,
                                            String baseName,
                                            Integer httpStatus,
                                            ParsedWebContent normalParsed,
                                            FetchDiagnostics diagnostics,
                                            boolean fallbackTriggered,
                                            RenderFetchResult renderFetch,
                                            ParsedWebContent renderParsed,
                                            String finalMode,
                                            ParsedWebContent finalParsed,
                                            HtmlFetchResult normalFetch) {
        String titleAttemptText = finalParsed.titleAttempts().isEmpty()
                ? "- 未执行标题提取"
                : String.join("\n", finalParsed.titleAttempts().stream()
                .map(item -> "- 尝试 " + item.strategy() + "：" + item.status() + (item.detail() == null || item.detail().isBlank() ? "" : "，" + item.detail()))
                .toList());
        String bodyAttemptText = finalParsed.bodyAttempts().isEmpty()
                ? "- 未执行正文提取"
                : String.join("\n", finalParsed.bodyAttempts().stream()
                .map(item -> {
                    StringBuilder builder = new StringBuilder("- 候选 ").append(item.strategy()).append("：");
                    if ("未命中".equals(item.status())) {
                        builder.append("未命中");
                    } else {
                        builder.append("长度 ").append(item.textLength()).append("，评分 ").append(item.score()).append("，判定").append(item.status());
                    }
                    if (item.reason() != null && !item.reason().isBlank()) {
                        builder.append("，原因：").append(item.reason());
                    }
                    return builder.toString();
                })
                .toList());
        String summaryStatus = finalParsed.summary() == null || finalParsed.summary().isBlank() ? "未生成" : "成功";
        String importDecision = finalParsed.success() ? "可导入" : "未导入";
        String failureType = classifyPreviewFailureType(diagnostics, normalParsed, renderFetch, renderParsed, finalParsed);
        String suggestion = finalParsed.success()
                ? "已成功命中正文主容器"
                : buildPreviewSuggestion(failureType, renderFetch, finalParsed);
        String requestStageText = """
请求阶段：
- 请求客户端：%s
- 请求开始时间：%s
- 请求目标地址：%s
- 最终请求地址：%s
- 是否发生重定向：%s
- 重定向次数：%s
- HTTP状态码：%s
- HTML长度：%s
- HTML预览：%s
- 异常类型：%s
- 异常消息：%s
- 是否超时：%s
- 是否 SSL 异常：%s
- 是否 DNS 解析异常：%s
- 是否连接被拒绝：%s
""".formatted(
                humanizeNull(normalFetch.clientType(), "未获取"),
                normalFetch.requestStartTime() == null ? "未获取" : normalFetch.requestStartTime().format(FEEDBACK_TIME_FORMATTER),
                humanizeNull(normalFetch.requestUrl(), "未获取"),
                humanizeNull(normalFetch.finalUrl(), "未获取"),
                normalFetch.redirected() ? "是" : "否",
                normalFetch.redirectCount(),
                httpStatus == null ? "未获取" : String.valueOf(httpStatus),
                normalFetch.htmlLength(),
                humanizeNull(normalFetch.htmlPreview(), "未获取"),
                humanizeNull(normalFetch.exceptionType(), "无"),
                humanizeNull(normalFetch.exceptionMessage(), "无"),
                normalFetch.timeout() ? "是" : "否",
                normalFetch.sslError() ? "是" : "否",
                normalFetch.dnsError() ? "是" : "否",
                normalFetch.connectionRefused() ? "是" : "否"
        );
        return """
【网页导入反馈信息】
时间：%s
请求地址：%s
所属知识库：%s
抓取结果：%s
抓取模式：%s
是否触发兜底：%s
HTTP状态：%s
HTML长度：%s
是否检测到 script 异步加载特征：%s
是否检测到 iframe：%s
是否检测到 rowId 等详情参数特征：%s
普通模式正文长度：%s
渲染模式正文长度：%s
最终采用模式：%s
失败类型：%s
页面title原始值：%s
发布时间：%s

%s

标题提取：
%s
- 最终标题：%s
- 最终标题策略：%s

正文提取：
%s
- 最终采用：%s
- 最终正文长度：%s

摘要生成：%s
导入结果：%s
建议：%s
""".formatted(
                requestTime.format(FEEDBACK_TIME_FORMATTER),
                humanizeNull(url, "未获取"),
                humanizeNull(baseName, "未选择"),
                finalParsed.success() ? "成功" : "失败",
                fallbackTriggered && "浏览器渲染".equals(finalMode) ? "浏览器渲染" : "普通HTTP",
                fallbackTriggered ? "是" : "否",
                httpStatus == null ? "未获取" : String.valueOf(httpStatus),
                normalParsed.htmlLength(),
                diagnostics.scriptAsyncDetected() ? "是" : "否",
                diagnostics.iframeDetected() ? "是" : "否",
                diagnostics.detailParamDetected() ? "是" : "否",
                normalParsed.content() == null ? 0 : normalParsed.content().length(),
                renderParsed == null || renderParsed.content() == null ? 0 : renderParsed.content().length(),
                finalMode,
                failureType,
                humanizeNull(finalParsed.pageTitle(), "未获取"),
                humanizeNull(finalParsed.publishTime(), "未获取"),
                requestStageText,
                titleAttemptText,
                humanizeNull(finalParsed.title(), "未获取"),
                humanizeNull(finalParsed.titleStrategy(), "未命中"),
                bodyAttemptText,
                humanizeNull(finalParsed.bodyStrategy(), "未命中"),
                finalParsed.content() == null ? 0 : finalParsed.content().length(),
                summaryStatus,
                importDecision,
                suggestion
        );
    }

    private String classifyPreviewFailureType(FetchDiagnostics diagnostics,
                                              ParsedWebContent normalParsed,
                                              RenderFetchResult renderFetch,
                                              ParsedWebContent renderParsed,
                                              ParsedWebContent finalParsed) {
        if (finalParsed.success()) {
            return "无";
        }
        if (diagnostics.requestFailed()) {
            return humanizeNull(diagnostics.note(), "其他未知请求异常");
        }
        if (normalParsed.htmlLength() == 0) {
            return "HTML为空";
        }
        if (normalParsed.htmlLength() > 0 && normalParsed.content() != null && normalParsed.content().isBlank()) {
            return "未识别到有效正文";
        }
        if (diagnostics.suspectedDynamicPage()) {
            return "疑似动态渲染";
        }
        if (diagnostics.detailParamDetected()) {
            return "疑似接口二次加载";
        }
        if (diagnostics.suspectedHeaderIssue()) {
            return "疑似访问头不足";
        }
        if (renderFetch.attempted() && !renderFetch.success()) {
            return "疑似站点限制";
        }
        if ((normalParsed.content() == null || normalParsed.content().isBlank())
                && (renderParsed == null || renderParsed.content() == null || renderParsed.content().isBlank())) {
            return "未识别到有效正文";
        }
        return humanizeNull(finalParsed.failureReason(), "未识别到有效正文");
    }

    private String buildPreviewSuggestion(String failureType,
                                          RenderFetchResult renderFetch,
                                          ParsedWebContent finalParsed) {
        if ("DNS解析失败".equals(failureType) || "连接超时".equals(failureType) || "读取超时".equals(failureType)
                || "SSL证书/握手异常".equals(failureType) || "连接被拒绝".equals(failureType)
                || "重定向异常".equals(failureType) || "返回4xx".equals(failureType)
                || "返回5xx".equals(failureType) || "HTML为空".equals(failureType)
                || "其他未知请求异常".equals(failureType)) {
            return "当前网页未成功抓取到内容，系统已自动尝试多种方式；如仍失败，请将反馈信息提供给技术人员处理。";
        }
        if ("疑似动态渲染".equals(failureType)) {
            return "当前页面疑似由脚本异步渲染，请优先查看渲染模式结果；如仍失败，可把反馈信息提供给技术人员继续补规则。";
        }
        if ("疑似接口二次加载".equals(failureType)) {
            return "当前页面疑似通过详情接口二次加载正文，建议技术人员结合反馈信息补充该站点的正文接口或选择器规则。";
        }
        if ("疑似访问头不足".equals(failureType)) {
            return "当前页面普通访问返回内容偏少，建议技术人员继续补浏览器请求头或改用浏览器渲染方式验证。";
        }
        if ("疑似站点限制".equals(failureType)) {
            return "当前站点可能存在访问限制或反抓取策略，建议改用公开详情页地址，或由技术人员进一步排查。";
        }
        if (renderFetch.attempted() && !renderFetch.success()) {
            return humanizeNull(renderFetch.errorReason(), "浏览器渲染未拿到有效页面内容");
        }
        return humanizeNull(finalParsed.failureReason(), "未识别到有效正文，请将反馈信息提供给技术人员继续优化。");
    }

    private String classifyHttpStatusFailure(Integer statusCode, String html) {
        if (statusCode == null) {
            return "HTTP状态未获取";
        }
        if (statusCode >= 500) {
            return "返回5xx";
        }
        if (statusCode >= 400) {
            return "返回4xx";
        }
        if (html == null || html.isBlank()) {
            return "HTML为空";
        }
        return null;
    }

    private RequestFailureInfo classifyRequestFailure(Exception ex) {
        Throwable root = rootCause(ex);
        String message = normalize(root.getMessage());
        if (root instanceof UnknownHostException) {
            return new RequestFailureInfo("DNS解析失败", root.getClass().getSimpleName(), humanizeNull(message, "域名无法解析"));
        }
        if (root instanceof HttpConnectTimeoutException) {
            return new RequestFailureInfo("连接超时", root.getClass().getSimpleName(), humanizeNull(message, "连接目标网站超时"));
        }
        if (root instanceof HttpTimeoutException) {
            return new RequestFailureInfo("读取超时", root.getClass().getSimpleName(), humanizeNull(message, "读取网页响应超时"));
        }
        if (root instanceof SSLException) {
            return new RequestFailureInfo("SSL证书/握手异常", root.getClass().getSimpleName(), humanizeNull(message, "SSL 握手或证书校验失败"));
        }
        if (root instanceof ConnectException && message != null && message.toLowerCase().contains("refused")) {
            return new RequestFailureInfo("连接被拒绝", root.getClass().getSimpleName(), message);
        }
        if (message != null && message.toLowerCase().contains("redirect")) {
            return new RequestFailureInfo("重定向异常", root.getClass().getSimpleName(), message);
        }
        return new RequestFailureInfo("其他未知请求异常", root.getClass().getSimpleName(), humanizeNull(message, humanizeWebImportError(ex)));
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private BodyExtractionResult extractBody(Document document, String title) {
        List<BodyAttempt> attempts = new ArrayList<>();
        IdentityHashMap<Element, Boolean> visited = new IdentityHashMap<>();
        BodyCandidate bestValid = null;
        BodyCandidate bestOverall = null;

        for (SelectorRule rule : BODY_SELECTOR_RULES) {
            Elements elements = document.select(rule.selector());
            if (elements.isEmpty()) {
                attempts.add(new BodyAttempt(rule.label(), "未命中", 0, 0, "未命中节点"));
                continue;
            }
            for (Element element : elements) {
                if (visited.put(element, Boolean.TRUE) != null) {
                    continue;
                }
                BodyCandidate candidate = evaluateBodyCandidate(rule.label(), element, title);
                attempts.add(candidate.attempt());
                if (bestOverall == null || candidate.score() > bestOverall.score()) {
                    bestOverall = candidate;
                }
                if (candidate.valid() && (bestValid == null || candidate.score() > bestValid.score())) {
                    bestValid = candidate;
                }
            }
        }

        List<Element> denseElements = document.body() == null
                ? List.of()
                : document.body().select("main, article, section, div, td");
        denseElements.sort(Comparator.comparingInt((Element element) -> cleanContentText(extractElementText(element)).length()).reversed());
        int denseCount = 0;
        for (Element element : denseElements) {
            if (denseCount >= 12) {
                break;
            }
            if (visited.put(element, Boolean.TRUE) != null) {
                continue;
            }
            BodyCandidate candidate = evaluateBodyCandidate("文本密度候选 " + describeElement(element), element, title);
            attempts.add(candidate.attempt());
            if (bestOverall == null || candidate.score() > bestOverall.score()) {
                bestOverall = candidate;
            }
            if (candidate.valid() && (bestValid == null || candidate.score() > bestValid.score())) {
                bestValid = candidate;
            }
            denseCount++;
        }

        if (bestValid != null) {
            return new BodyExtractionResult(bestValid.text(), bestValid.strategy(), true, attempts, null);
        }
        String failureReason = bestOverall == null || bestOverall.reason() == null || bestOverall.reason().isBlank()
                ? "未提取到有效正文"
                : bestOverall.reason();
        return new BodyExtractionResult(bestOverall == null ? "" : bestOverall.text(), "", false, attempts, failureReason);
    }

    private BodyCandidate evaluateBodyCandidate(String strategy, Element element, String title) {
        String cleanedText = cleanContentText(extractElementText(element));
        int textLength = cleanedText.length();
        if (textLength == 0) {
            return new BodyCandidate(strategy, "", 0, false, "正文为空", new BodyAttempt(strategy, "低质量", 0, 0, "正文为空"));
        }
        int chineseCount = countChineseChars(cleanedText);
        int punctuationCount = countPunctuation(cleanedText);
        int paragraphCount = countParagraphs(cleanedText);
        int navKeywordHits = countNoiseKeywordHits(cleanedText);
        double chineseRatio = textLength == 0 ? 0D : (double) chineseCount / textLength;
        int score = Math.min(textLength / 40, 80)
                + Math.min(chineseCount / 30, 60)
                + Math.min(paragraphCount * 4, 28)
                + Math.min(punctuationCount * 2, 24)
                - navKeywordHits * 10;

        List<String> reasons = new ArrayList<>();
        if (textLength < 120) {
            reasons.add("正文过短");
        }
        if (chineseCount < 60) {
            reasons.add("中文内容过少");
        }
        if (chineseRatio < 0.25D) {
            reasons.add("中文占比过低");
        }
        if (navKeywordHits >= 3 && textLength < 400) {
            reasons.add("导航词占比过高");
        }
        if (paragraphCount < 2 && textLength < 280) {
            reasons.add("段落结构不足");
        }
        if (punctuationCount < 2 && textLength < 200) {
            reasons.add("正文标点过少");
        }
        if (title != null && !title.isBlank() && cleanedText.equals(title)) {
            reasons.add("正文与标题重复");
        }

        boolean valid = reasons.isEmpty();
        String reason = valid ? "命中正文主区域" : String.join("、", reasons);
        BodyAttempt attempt = new BodyAttempt(strategy, valid ? "通过" : "低质量", textLength, Math.max(score, 0), reason);
        return new BodyCandidate(strategy, cleanedText, Math.max(score, 0), valid, reason, attempt);
    }

    private String extractElementText(Element element) {
        String text = normalize(element.wholeText());
        if (text != null) {
            return text;
        }
        return humanizeNull(normalize(element.text()), "");
    }

    private String cleanContentText(String rawText) {
        String normalizedText = normalize(rawText);
        if (normalizedText == null) {
            return "";
        }
        String prepared = normalizedText
                .replace("\u00A0", " ")
                .replaceAll("\\r\\n?", "\n")
                .replaceAll("[\\t\\f]+", " ")
                .replaceAll(" {2,}", " ");
        List<String> cleanedLines = new ArrayList<>();
        String previous = null;
        for (String line : prepared.split("\n")) {
            String normalizedLine = normalize(line);
            if (normalizedLine == null || isNoiseLine(normalizedLine)) {
                continue;
            }
            if (normalizedLine.equals(previous)) {
                continue;
            }
            cleanedLines.add(normalizedLine);
            previous = normalizedLine;
        }
        return String.join("\n", cleanedLines).replaceAll("\\n{3,}", "\n\n").trim();
    }

    private boolean isNoiseLine(String line) {
        String text = normalize(line);
        if (text == null) {
            return true;
        }
        String compact = text.replace(" ", "");
        if (compact.length() <= 2) {
            return true;
        }
        String lower = compact.toLowerCase();
        for (String keyword : NOISE_LINE_KEYWORDS) {
            String normalizedKeyword = keyword.toLowerCase();
            if (lower.equals(normalizedKeyword)) {
                return true;
            }
            if (lower.contains(normalizedKeyword) && compact.length() <= 18) {
                return true;
            }
        }
        return compact.matches("^[>/>\\-|]+$");
    }

    private String cleanTitle(String rawTitle) {
        String title = normalize(rawTitle);
        if (title == null) {
            return null;
        }
        return title.replaceAll("\\s+", " ").trim();
    }

    private boolean isValidTitleCandidate(String candidate) {
        String title = cleanTitle(candidate);
        if (title == null || title.length() < 6) {
            return false;
        }
        String compact = title.replace(" ", "");
        if (compact.length() < 6) {
            return false;
        }
        String lower = compact.toLowerCase();
        for (String keyword : NOISE_LINE_KEYWORDS) {
            String normalizedKeyword = keyword.toLowerCase();
            if (lower.equals(normalizedKeyword)) {
                return false;
            }
            if (lower.contains(normalizedKeyword) && compact.length() <= 24) {
                return false;
            }
        }
        return countChineseChars(title) >= 4 || title.length() >= 12;
    }

    private String extractBestTitleFromPageTitle(String rawPageTitle) {
        String title = cleanTitle(rawPageTitle);
        if (title == null) {
            return null;
        }
        if (isValidTitleCandidate(title)) {
            return title;
        }
        String[] segments = title.split("\\s*[\\-|_|丨|—|–|·]\\s*");
        String best = null;
        int bestChinese = -1;
        for (String segment : segments) {
            String candidate = cleanTitle(segment);
            if (!isValidTitleCandidate(candidate)) {
                continue;
            }
            int chineseCount = countChineseChars(candidate);
            if (chineseCount > bestChinese) {
                bestChinese = chineseCount;
                best = candidate;
            }
        }
        return best;
    }

    private String extractTitleFromContentFirstLine(String content) {
        String normalizedContent = normalize(content);
        if (normalizedContent == null) {
            return null;
        }
        for (String line : normalizedContent.split("\n")) {
            String candidate = cleanTitle(line);
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    private String buildSummary(String content) {
        String normalizedContent = normalize(content);
        if (normalizedContent == null) {
            return "";
        }
        return normalizedContent.substring(0, Math.min(normalizedContent.length(), 160));
    }

    private String extractPublishTime(Document document) {
        for (String selector : List.of(".time", ".publish-time", ".pub-time", ".info", ".meta", ".detail-info", ".article-info", ".news-info")) {
            for (Element element : document.select(selector)) {
                String matched = matchPublishTime(element.text());
                if (matched != null) {
                    return matched;
                }
            }
        }
        return matchPublishTime(document.text());
    }

    private String matchPublishTime(String text) {
        String normalizedText = normalize(text);
        if (normalizedText == null) {
            return null;
        }
        Matcher matcher = PUBLISH_TIME_PATTERN.matcher(normalizedText);
        if (matcher.find()) {
            return matcher.group(1).replace('年', '-').replace('月', '-').replace("日", "");
        }
        return null;
    }

    private int countChineseChars(String text) {
        if (text == null) {
            return 0;
        }
        int count = 0;
        for (char ch : text.toCharArray()) {
            if (ch >= 0x4E00 && ch <= 0x9FFF) {
                count++;
            }
        }
        return count;
    }

    private int countPunctuation(String text) {
        if (text == null) {
            return 0;
        }
        int count = 0;
        for (char ch : text.toCharArray()) {
            if ("，。；：！？、,.!?;:".indexOf(ch) >= 0) {
                count++;
            }
        }
        return count;
    }

    private int countParagraphs(String text) {
        String normalizedText = normalize(text);
        if (normalizedText == null) {
            return 0;
        }
        return (int) java.util.Arrays.stream(normalizedText.split("\n+"))
                .map(this::normalize)
                .filter(Objects::nonNull)
                .count();
    }

    private int countNoiseKeywordHits(String text) {
        String normalizedText = normalize(text);
        if (normalizedText == null) {
            return 0;
        }
        String lower = normalizedText.toLowerCase();
        int count = 0;
        for (String keyword : NOISE_LINE_KEYWORDS) {
            if (lower.contains(keyword.toLowerCase())) {
                count++;
            }
        }
        return count;
    }

    private String describeElement(Element element) {
        StringBuilder builder = new StringBuilder(element.tagName());
        if (element.id() != null && !element.id().isBlank()) {
            builder.append('#').append(element.id());
        }
        if (!element.classNames().isEmpty()) {
            builder.append('.').append(String.join(".", element.classNames().stream().limit(2).toList()));
        }
        return builder.toString();
    }

    private String buildImportFeedbackText(LocalDateTime requestTime,
                                           String url,
                                           String baseName,
                                           String title,
                                           int contentLength,
                                           Long documentId,
                                           String errorReason) {
        boolean success = errorReason == null;
        return """
【网页导入反馈信息】
时间：%s
请求地址：%s
所属知识库：%s
抓取结果：%s
HTTP状态：%s
页面标题：%s
正文提取长度：%s
摘要生成：成功
导入结果：%s
知识条目ID：%s
备注：%s
""".formatted(
                requestTime.format(FEEDBACK_TIME_FORMATTER),
                humanizeNull(url, "未获取"),
                humanizeNull(baseName, "未选择"),
                "成功",
                "预览阶段已完成",
                humanizeNull(title, "未获取"),
                contentLength,
                success ? "成功" : "失败",
                success ? humanizeNull(documentId == null ? null : String.valueOf(documentId), "未生成") : "未导入",
                success ? "网页内容已保存到知识库，可继续在文档列表中查看。" : humanizeNull(errorReason, "导入失败")
        );
    }

    private String buildSnapshotPreviewFeedbackText(LocalDateTime requestTime,
                                                    String fileName,
                                                    String extension,
                                                    long fileSize,
                                                    String baseName,
                                                    SnapshotExtractResult extractResult,
                                                    ParsedWebContent parsed) {
        String titleStatus = parsed.title() == null || parsed.title().isBlank() ? "未获取" : parsed.title();
        String titleStrategy = parsed.titleStrategy() == null || parsed.titleStrategy().isBlank() ? "未命中" : parsed.titleStrategy();
        String bodyStatus = parsed.content() == null || parsed.content().isBlank() ? "低质量/未获取" : "通过";
        String bodyReason = parsed.failureReason() == null || parsed.failureReason().isBlank() ? "正文提取正常" : parsed.failureReason();
        return """
【网页快照导入反馈信息】
时间：%s
文件名：%s
文件类型：%s
文件大小：%s KB
所属知识库：%s

快照解析：
- 是否识别为MHTML：%s
- 主HTML提取：%s
- 主HTML长度：%s
- 来源信息：%s
- 解析备注：%s
- 失败原因：%s

标题提取：
- 最终标题：%s
- 标题策略：%s

正文提取：
- 最终正文长度：%s
- 判定结果：%s
- 说明：%s

摘要生成：%s
导入结果：%s
建议：%s
""".formatted(
                requestTime.format(FEEDBACK_TIME_FORMATTER),
                humanizeNull(fileName, "未获取"),
                extension.toLowerCase(),
                Math.max(1, fileSize / 1024),
                humanizeNull(baseName, "未选择"),
                extractResult.mhtml() ? "是" : "否",
                extractResult.mainHtmlExtracted() ? "成功" : "失败",
                extractResult.htmlLength(),
                humanizeNull(extractResult.sourceUrl(), "未获取"),
                humanizeNull(extractResult.note(), "无"),
                humanizeNull(extractResult.failureReason(), "无"),
                humanizeNull(titleStatus, "未获取"),
                titleStrategy,
                parsed.content() == null ? 0 : parsed.content().length(),
                bodyStatus,
                bodyReason,
                parsed.summary() == null || parsed.summary().isBlank() ? "未生成" : "成功",
                parsed.success() ? "可导入" : "未导入",
                parsed.success()
                        ? "网页快照解析成功，可在确认内容后导入知识库。"
                        : "请确认页面已完整加载后重新另存为 .mhtml 或 .html，再重新上传解析。"
        );
    }

    private String buildSnapshotImportFeedbackText(LocalDateTime requestTime,
                                                   String fileName,
                                                   String extension,
                                                   long fileSize,
                                                   String baseName,
                                                   String title,
                                                   int contentLength,
                                                   Long documentId,
                                                   String errorReason) {
        boolean success = errorReason == null;
        return """
【网页快照导入反馈信息】
时间：%s
文件名：%s
文件类型：%s
文件大小：%s KB
所属知识库：%s

快照解析：
- 是否识别为MHTML：%s
- 主HTML提取：%s

标题提取：
- 最终标题：%s

正文提取：
- 最终正文长度：%s

摘要生成：成功
导入结果：%s
知识条目ID：%s
备注：%s
""".formatted(
                requestTime.format(FEEDBACK_TIME_FORMATTER),
                humanizeNull(fileName, "未获取"),
                extension.toLowerCase(),
                Math.max(1, fileSize / 1024),
                humanizeNull(baseName, "未选择"),
                "mhtml".equals(extension) ? "是" : "否",
                "成功",
                humanizeNull(title, "未获取"),
                contentLength,
                success ? "成功" : "失败",
                success ? humanizeNull(documentId == null ? null : String.valueOf(documentId), "未生成") : "未导入",
                success ? "网页快照内容已保存到知识库，可继续在文档列表中查看。" : humanizeNull(errorReason, "导入失败")
        );
    }

    private String humanizeWebImportError(Exception ex) {
        String message = normalize(ex.getMessage());
        if (message == null) {
            return "网页抓取失败，请检查地址是否可公开访问";
        }
        if (message.contains("403")) {
            return "目标网页拒绝访问或存在反爬限制";
        }
        if (message.contains("404")) {
            return "目标网页不存在或地址已失效";
        }
        if (message.contains("URI")) {
            return "网页地址格式不正确，请检查后重试";
        }
        return "网页抓取失败，请检查地址是否可公开访问";
    }

    private String humanizeNull(String value, String fallback) {
        String normalized = normalize(value);
        return normalized == null ? fallback : normalized;
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

    private record SelectorRule(String label, String selector) {
    }

    private record WebFetchResult(boolean success,
                                  String title,
                                  String summary,
                                  String content,
                                  String sourceUrl,
                                  String feedbackText) {
    }

    private record SnapshotPreviewResult(boolean success,
                                         String title,
                                         String summary,
                                         String content,
                                         String sourceUrl,
                                         String fileName,
                                         String feedbackText) {
    }

    private record SnapshotExtractResult(boolean success,
                                         boolean mhtml,
                                         boolean mainHtmlExtracted,
                                         String html,
                                         String sourceUrl,
                                         String transferEncoding,
                                         String note,
                                         String failureReason) {
        private int htmlLength() {
            return html == null ? 0 : html.length();
        }
    }

    private record HtmlFetchResult(boolean success,
                                   String clientType,
                                   LocalDateTime requestStartTime,
                                   String requestUrl,
                                   Integer statusCode,
                                   String finalUrl,
                                   String html,
                                   boolean redirected,
                                   int redirectCount,
                                   String exceptionType,
                                   String exceptionMessage,
                                   String failureType,
                                   boolean timeout,
                                   boolean sslError,
                                   boolean dnsError,
                                   boolean connectionRefused) {
        private static HtmlFetchResult failed(String clientType,
                                              LocalDateTime requestStartTime,
                                              String requestUrl,
                                              String finalUrl,
                                              String failureType,
                                              Exception ex) {
            Throwable root = ex;
            while (root.getCause() != null && root.getCause() != root) {
                root = root.getCause();
            }
            String message = root.getMessage() == null ? ex.getMessage() : root.getMessage();
            return new HtmlFetchResult(
                    false,
                    clientType,
                    requestStartTime,
                    requestUrl,
                    null,
                    finalUrl,
                    "",
                    false,
                    0,
                    root.getClass().getSimpleName(),
                    message == null ? "" : message,
                    failureType,
                    root instanceof HttpTimeoutException,
                    root instanceof SSLException,
                    root instanceof UnknownHostException,
                    root instanceof ConnectException && message != null && message.toLowerCase().contains("refused")
            );
        }

        private int htmlLength() {
            return html == null ? 0 : html.length();
        }

        private String htmlPreview() {
            if (html == null || html.isBlank()) {
                return "";
            }
            String compact = html.replaceAll("\\s+", " ").trim();
            return compact.substring(0, Math.min(compact.length(), 400));
        }
    }

    private record FetchDiagnostics(boolean requestFailed,
                                    boolean htmlEmpty,
                                    boolean shortHtml,
                                    boolean titleMissing,
                                    boolean contentEmpty,
                                    boolean scriptAsyncDetected,
                                    boolean iframeDetected,
                                    boolean detailParamDetected,
                                    boolean suspectedDynamicPage,
                                    boolean suspectedHeaderIssue,
                                    int normalBodyLength,
                                    String note) {
        private static FetchDiagnostics failed(String note) {
            return new FetchDiagnostics(true, true, false, true, true, false, false, false, false, false, 0, note);
        }
    }

    private record RenderFetchResult(boolean attempted,
                                     boolean success,
                                     String browserPath,
                                     String finalUrl,
                                     String html,
                                     String errorReason) {
        private static RenderFetchResult notTriggered() {
            return new RenderFetchResult(false, false, "", "", "", "");
        }

        private int htmlLength() {
            return html == null ? 0 : html.length();
        }
    }

    private record ModeSelection(String finalMode,
                                 String finalUrl,
                                 ParsedWebContent finalParsed) {
    }

    private record RequestFailureInfo(String failureType,
                                      String exceptionType,
                                      String exceptionMessage) {
    }

    private record TitleAttempt(String strategy, String status, String detail) {
    }

    private record TitleExtractionResult(String title,
                                         String strategy,
                                         List<TitleAttempt> attempts) {
    }

    private record BodyAttempt(String strategy,
                               String status,
                               int textLength,
                               int score,
                               String reason) {
    }

    private record BodyCandidate(String strategy,
                                 String text,
                                 int score,
                                 boolean valid,
                                 String reason,
                                 BodyAttempt attempt) {
    }

    private record BodyExtractionResult(String content,
                                        String strategy,
                                        boolean valid,
                                        List<BodyAttempt> attempts,
                                        String failureReason) {
    }

    private record ParsedWebContent(String title,
                                    String summary,
                                    String content,
                                    boolean success,
                                    String pageTitle,
                                    int htmlLength,
                                    String titleStrategy,
                                    String bodyStrategy,
                                    String publishTime,
                                    List<TitleAttempt> titleAttempts,
                                    List<BodyAttempt> bodyAttempts,
                                    String failureReason) {
    }
}
