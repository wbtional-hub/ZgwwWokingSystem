package com.example.lecturesystem.modules.orgtree.service.impl;

import com.example.lecturesystem.modules.auth.security.LoginUser;
import com.example.lecturesystem.modules.orgtree.dto.CreateChildUserRequest;
import com.example.lecturesystem.modules.orgtree.dto.MoveNodeRequest;
import com.example.lecturesystem.modules.orgtree.dto.ToggleOrgNodeStatusRequest;
import com.example.lecturesystem.modules.orgtree.dto.UpdateOrgNodeRequest;
import com.example.lecturesystem.modules.orgtree.entity.OrgNodeEntity;
import com.example.lecturesystem.modules.orgtree.mapper.OrgTreeMapper;
import com.example.lecturesystem.modules.orgtree.service.OrgTreeService;
import com.example.lecturesystem.modules.orgtree.vo.OrgTreeNodeVO;
import com.example.lecturesystem.modules.permission.mapper.PermissionMapper;
import com.example.lecturesystem.modules.permission.service.PermissionService;
import com.example.lecturesystem.modules.user.entity.UserEntity;
import com.example.lecturesystem.modules.user.mapper.UserMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.time.LocalDateTime;

@Service
public class OrgTreeServiceImpl implements OrgTreeService {
    private static final String ROLE_ORG_USER = "ORG_USER";

    private final OrgTreeMapper orgTreeMapper;
    private final UserMapper userMapper;
    private final PermissionService permissionService;
    private final PermissionMapper permissionMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public OrgTreeServiceImpl(OrgTreeMapper orgTreeMapper,
                              UserMapper userMapper,
                              PermissionService permissionService,
                              PermissionMapper permissionMapper) {
        this.orgTreeMapper = orgTreeMapper;
        this.userMapper = userMapper;
        this.permissionService = permissionService;
        this.permissionMapper = permissionMapper;
    }

    @Override
    @Transactional
    public Long createChild(CreateChildUserRequest request) {
        LoginUser loginUser = currentLoginUser();
        UserEntity parentUser = requireUser(request.getParentUserId(), "上级用户不存在");
        OrgNodeEntity parentNode = ensureParentNodeForCreate(loginUser.getUserId(), request.getParentUserId(), parentUser);
        validateCreatePermission(loginUser.getUserId(), parentNode);

        if (userMapper.findByUsername(request.getUsername()) != null) {
            throw new IllegalArgumentException("账号已存在");
        }

        UserEntity newUser = new UserEntity();
        Long targetUnitId = request.getUnitId() != null ? request.getUnitId() : parentNode.getUnitId();
        newUser.setUnitId(targetUnitId);
        newUser.setUsername(request.getUsername());
        newUser.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        newUser.setRealName(request.getRealName());
        newUser.setJobTitle(request.getJobTitle());
        newUser.setMobile(request.getMobile());
        newUser.setRole("USER");
        newUser.setStatus(request.getStatus());
        newUser.setCreateTime(LocalDateTime.now());
        newUser.setUpdateTime(LocalDateTime.now());
        newUser.setCreateUser(loginUser.getUsername());
        newUser.setUpdateUser(loginUser.getUsername());
        newUser.setIsDeleted(Boolean.FALSE);
        userMapper.insertUser(newUser);
        orgTreeMapper.updateUserNodeInfo(
                newUser.getId(),
                parentUser.getId(),
                targetUnitId,
                parentNode.getLevelNo() + 1,
                ensureTrailingSlash(parentNode.getTreePath()) + newUser.getId() + "/"
        );

        // 最小闭环阶段，新建下级默认绑定为普通组织用户。
        permissionMapper.insertUserRole(newUser.getId(), ROLE_ORG_USER);
        return newUser.getId();
    }

    @Override
    public Object queryTree() {
        LoginUser loginUser = currentLoginUser();
        UserEntity currentUser = requireUser(loginUser.getUserId(), "当前用户不存在");
        OrgNodeEntity currentNode = ensureCurrentNode(currentUser);
        List<OrgTreeNodeVO> nodeList = orgTreeMapper.querySubtreeNodes(
                currentUser.getUnitId(),
                ensureTrailingSlash(currentNode.getTreePath()) + "%"
        );
        return buildTree(nodeList, currentNode.getUserId());
    }

    @Override
    public Object queryChildren(Long userId) {
        LoginUser loginUser = currentLoginUser();
        UserEntity currentUser = requireUser(loginUser.getUserId(), "当前用户不存在");
        ensureCurrentNode(currentUser);

        UserEntity parentUser = requireUser(userId, "用户不存在");
        if (!permissionService.isSuperAdmin(loginUser.getUserId())) {
            Set<Long> scopeUserIds = permissionService.queryDataScopeUserIds(loginUser.getUserId());
            if (!scopeUserIds.contains(userId)) {
                throw new IllegalArgumentException("无权查看该节点下级");
            }
        }

        return orgTreeMapper.queryChildrenNodes(parentUser.getUnitId(), userId);
    }

    @Override
    public Object queryAncestors(Long userId) {
        LoginUser loginUser = currentLoginUser();
        UserEntity currentUser = requireUser(loginUser.getUserId(), "当前用户不存在");
        ensureCurrentNode(currentUser);

        UserEntity targetUser = requireUser(userId, "用户不存在");
        if (!permissionService.isSuperAdmin(loginUser.getUserId())) {
            Set<Long> scopeUserIds = permissionService.queryDataScopeUserIds(loginUser.getUserId());
            if (!scopeUserIds.contains(userId)) {
                throw new IllegalArgumentException("无权查看该用户上级链");
            }
        }

        OrgNodeEntity targetNode = requireNode(userId, "当前节点不存在");
        return orgTreeMapper.queryAncestorNodes(targetUser.getUnitId(), targetNode.getTreePath());
    }

    @Override
    @Transactional
    public void moveNode(MoveNodeRequest request) {
        LoginUser loginUser = currentLoginUser();
        OrgNodeEntity currentNode = requireNode(request.getUserId(), "当前节点不存在");
        OrgNodeEntity targetParentNode = requireNode(request.getTargetParentUserId(), "目标上级不存在");

        if (Objects.equals(currentNode.getUserId(), targetParentNode.getUserId())) {
            throw new IllegalArgumentException("不能移动到自己下面");
        }
        if (!Objects.equals(currentNode.getUnitId(), targetParentNode.getUnitId())) {
            throw new IllegalArgumentException("只能在同一单位内移动节点");
        }
        String currentTreePath = ensureTrailingSlash(currentNode.getTreePath());
        String targetParentTreePath = ensureTrailingSlash(targetParentNode.getTreePath());
        if (targetParentTreePath.equals(currentTreePath)
                || targetParentTreePath.startsWith(currentTreePath)) {
            throw new IllegalArgumentException("不能移动到自己或自己的下级下面");
        }

        validateMovePermission(loginUser.getUserId(), currentNode, targetParentNode);

        String newTreePath = targetParentTreePath + currentNode.getUserId() + "/";
        int levelOffset = targetParentNode.getLevelNo() + 1 - currentNode.getLevelNo();
        orgTreeMapper.updateSubtreeAfterMove(
                currentNode.getUserId(),
                currentTreePath,
                newTreePath,
                levelOffset,
                targetParentNode.getUserId()
        );
    }

    @Override
    @Transactional
    public void updateNode(UpdateOrgNodeRequest request) {
        LoginUser loginUser = currentLoginUser();
        UserEntity targetUser = requireUser(request.getUserId(), "当前节点不存在");
        OrgNodeEntity targetNode = requireNode(request.getUserId(), "当前节点不存在");
        validateOperatePermission(loginUser.getUserId(), targetNode);

        targetUser.setRealName(request.getRealName().trim());
        targetUser.setJobTitle(trimToNull(request.getJobTitle()));
        targetUser.setMobile(trimToNull(request.getMobile()));
        if (request.getUnitId() != null) {
            targetUser.setUnitId(request.getUnitId());
        }
        targetUser.setUpdateTime(LocalDateTime.now());
        targetUser.setUpdateUser(loginUser.getUsername());
        userMapper.updateUser(targetUser);
    }

    @Override
    @Transactional
    public void deleteNode(Long userId) {
        LoginUser loginUser = currentLoginUser();
        if (Objects.equals(loginUser.getUserId(), userId)) {
            throw new IllegalArgumentException("不能删除当前登录用户");
        }

        UserEntity targetUser = requireUser(userId, "当前节点不存在");
        OrgNodeEntity targetNode = requireNode(userId, "当前节点不存在");
        validateOperatePermission(loginUser.getUserId(), targetNode);

        List<OrgTreeNodeVO> children = orgTreeMapper.queryChildrenNodes(targetUser.getUnitId(), userId);
        if (!children.isEmpty()) {
            throw new IllegalArgumentException("请先删除或转移下级节点");
        }

        userMapper.logicalDelete(userId, loginUser.getUsername(), LocalDateTime.now());
    }

    @Override
    @Transactional
    public void toggleNodeStatus(ToggleOrgNodeStatusRequest request) {
        LoginUser loginUser = currentLoginUser();
        if (Objects.equals(loginUser.getUserId(), request.getUserId()) && request.getStatus() != null && request.getStatus() == 0) {
            throw new IllegalArgumentException("不能停用当前登录用户");
        }

        UserEntity targetUser = requireUser(request.getUserId(), "当前节点不存在");
        OrgNodeEntity targetNode = requireNode(request.getUserId(), "当前节点不存在");
        validateOperatePermission(loginUser.getUserId(), targetNode);

        targetUser.setStatus(request.getStatus());
        targetUser.setUpdateTime(LocalDateTime.now());
        targetUser.setUpdateUser(loginUser.getUsername());
        userMapper.updateUser(targetUser);
    }

    private OrgNodeEntity ensureParentNodeForCreate(Long currentUserId, Long parentUserId, UserEntity parentUser) {
        OrgNodeEntity parentNode = orgTreeMapper.findByUserId(parentUserId);
        if (parentNode != null && parentNode.getLevelNo() != null && parentNode.getTreePath() != null) {
            return parentNode;
        }

        if (Objects.equals(currentUserId, parentUserId)) {
            return initRootNode(parentUser);
        }

        throw new IllegalArgumentException("上级节点不存在");
    }

    private OrgNodeEntity ensureCurrentNode(UserEntity currentUser) {
        OrgNodeEntity currentNode = orgTreeMapper.findByUserId(currentUser.getId());
        if (currentNode != null && currentNode.getLevelNo() != null && currentNode.getTreePath() != null) {
            return currentNode;
        }
        return initRootNode(currentUser);
    }

    private OrgNodeEntity initRootNode(UserEntity user) {
        orgTreeMapper.updateUserNodeInfo(user.getId(), null, user.getUnitId(), 1, "/" + user.getId() + "/");
        OrgNodeEntity rootNode = new OrgNodeEntity();
        rootNode.setUserId(user.getId());
        rootNode.setUnitId(user.getUnitId());
        rootNode.setParentUserId(null);
        rootNode.setLevelNo(1);
        rootNode.setTreePath("/" + user.getId() + "/");
        return rootNode;
    }

    private String ensureTrailingSlash(String treePath) {
        if (treePath == null || treePath.isBlank()) {
            return "/";
        }
        return treePath.endsWith("/") ? treePath : treePath + "/";
    }

    private void validateCreatePermission(Long currentUserId, OrgNodeEntity parentNode) {
        if (permissionService.isSuperAdmin(currentUserId)) {
            return;
        }

        UserEntity currentUser = requireUser(currentUserId, "当前用户不存在");
        if (!Objects.equals(currentUser.getUnitId(), parentNode.getUnitId())) {
            throw new IllegalArgumentException("无权操作其他单位的组织树");
        }

        if (!Objects.equals(currentUserId, parentNode.getUserId())) {
            throw new IllegalArgumentException("当前阶段仅允许在本人节点下新增直属下级");
        }
    }

    private void validateMovePermission(Long currentUserId, OrgNodeEntity currentNode, OrgNodeEntity targetParentNode) {
        if (permissionService.isSuperAdmin(currentUserId)) {
            return;
        }

        UserEntity currentUser = requireUser(currentUserId, "当前用户不存在");
        if (!Objects.equals(currentUser.getUnitId(), currentNode.getUnitId())
                || !Objects.equals(currentUser.getUnitId(), targetParentNode.getUnitId())) {
            throw new IllegalArgumentException("无权操作其他单位的组织树");
        }

        Set<Long> scopeUserIds = permissionService.queryDataScopeUserIds(currentUserId);
        if (!scopeUserIds.contains(currentNode.getUserId()) || !scopeUserIds.contains(targetParentNode.getUserId())) {
            throw new IllegalArgumentException("只能移动本人可管理范围内的节点");
        }
    }

    private void validateOperatePermission(Long currentUserId, OrgNodeEntity targetNode) {
        if (permissionService.isSuperAdmin(currentUserId)) {
            return;
        }

        UserEntity currentUser = requireUser(currentUserId, "当前用户不存在");
        if (!Objects.equals(currentUser.getUnitId(), targetNode.getUnitId())) {
            throw new IllegalArgumentException("无权操作其他单位的组织树");
        }

        Set<Long> scopeUserIds = permissionService.queryDataScopeUserIds(currentUserId);
        if (!scopeUserIds.contains(targetNode.getUserId())) {
            throw new IllegalArgumentException("只能操作本人可管理范围内的节点");
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private List<OrgTreeNodeVO> buildTree(List<OrgTreeNodeVO> nodes, Long rootUserId) {
        Map<Long, OrgTreeNodeVO> nodeMap = new LinkedHashMap<>();
        for (OrgTreeNodeVO node : nodes) {
            node.getChildren().clear();
            nodeMap.put(node.getUserId(), node);
        }

        List<OrgTreeNodeVO> roots = new ArrayList<>();
        for (OrgTreeNodeVO node : nodes) {
            Long parentId = node.getParentUserId();
            if (parentId != null && nodeMap.containsKey(parentId)) {
                nodeMap.get(parentId).getChildren().add(node);
            } else {
                roots.add(node);
            }
        }

        sortTree(roots);
        OrgTreeNodeVO root = nodeMap.get(rootUserId);
        if (root == null) {
            throw new IllegalArgumentException("当前用户节点不存在");
        }
        return List.of(root);
    }

    private void sortTree(List<OrgTreeNodeVO> nodes) {
        nodes.sort(Comparator.comparing(OrgTreeNodeVO::getLevelNo).thenComparing(OrgTreeNodeVO::getUserId));
        for (OrgTreeNodeVO node : nodes) {
            sortTree(node.getChildren());
        }
    }

    private OrgNodeEntity requireNode(Long userId, String message) {
        OrgNodeEntity node = orgTreeMapper.findByUserId(userId);
        if (node == null) {
            throw new IllegalArgumentException(message);
        }
        return node;
    }

    private UserEntity requireUser(Long userId, String message) {
        UserEntity user = userMapper.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException(message);
        }
        return user;
    }

    private LoginUser currentLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser loginUser)) {
            throw new IllegalArgumentException("当前未登录");
        }
        return loginUser;
    }
}
