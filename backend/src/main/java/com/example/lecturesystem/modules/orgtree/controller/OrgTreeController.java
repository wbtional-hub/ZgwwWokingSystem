package com.example.lecturesystem.modules.orgtree.controller;

import com.example.lecturesystem.common.ApiResponse;
import com.example.lecturesystem.modules.orgtree.dto.CreateChildUserRequest;
import com.example.lecturesystem.modules.orgtree.dto.MoveNodeRequest;
import com.example.lecturesystem.modules.orgtree.dto.ToggleOrgNodeStatusRequest;
import com.example.lecturesystem.modules.orgtree.dto.UpdateOrgNodeRequest;
import com.example.lecturesystem.modules.orgtree.service.OrgTreeService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/orgtree", "/api/org-tree"})
public class OrgTreeController {
    private final OrgTreeService orgTreeService;

    public OrgTreeController(OrgTreeService orgTreeService) {
        this.orgTreeService = orgTreeService;
    }

    @PostMapping({"/create", "/create-child"})
    public ApiResponse<?> create(@Validated @RequestBody CreateChildUserRequest request) {
        return ApiResponse.success(orgTreeService.createChild(request));
    }

    @PostMapping("/move")
    public ApiResponse<?> moveNode(@Validated @RequestBody MoveNodeRequest request) {
        orgTreeService.moveNode(request);
        return ApiResponse.success("ok");
    }

    @PostMapping("/update")
    public ApiResponse<?> updateNode(@Validated @RequestBody UpdateOrgNodeRequest request) {
        orgTreeService.updateNode(request);
        return ApiResponse.success("ok");
    }

    @PostMapping("/toggle-status")
    public ApiResponse<?> toggleStatus(@Validated @RequestBody ToggleOrgNodeStatusRequest request) {
        orgTreeService.toggleNodeStatus(request);
        return ApiResponse.success("ok");
    }

    @DeleteMapping("/{userId}")
    public ApiResponse<?> deleteNode(@PathVariable Long userId) {
        orgTreeService.deleteNode(userId);
        return ApiResponse.success("ok");
    }

    @GetMapping("/tree")
    public ApiResponse<?> tree() {
        return ApiResponse.success(orgTreeService.queryTree());
    }

    @GetMapping("/children/{userId}")
    public ApiResponse<?> children(@PathVariable("userId") Long userId) {
        return ApiResponse.success(orgTreeService.queryChildren(userId));
    }

    @GetMapping("/ancestors/{userId}")
    public ApiResponse<?> ancestors(@PathVariable("userId") Long userId) {
        return ApiResponse.success(orgTreeService.queryAncestors(userId));
    }
}
