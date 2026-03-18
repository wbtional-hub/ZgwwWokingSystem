package com.example.lecturesystem.modules.orgtree.service;

import com.example.lecturesystem.modules.orgtree.dto.CreateChildUserRequest;
import com.example.lecturesystem.modules.orgtree.dto.MoveNodeRequest;
import com.example.lecturesystem.modules.orgtree.dto.ToggleOrgNodeStatusRequest;
import com.example.lecturesystem.modules.orgtree.dto.UpdateOrgNodeRequest;

public interface OrgTreeService {
    Long createChild(CreateChildUserRequest request);
    Object queryTree();
    Object queryChildren(Long userId);
    Object queryAncestors(Long userId);
    void moveNode(MoveNodeRequest request);
    void updateNode(UpdateOrgNodeRequest request);
    void deleteNode(Long userId);
    void toggleNodeStatus(ToggleOrgNodeStatusRequest request);
}
