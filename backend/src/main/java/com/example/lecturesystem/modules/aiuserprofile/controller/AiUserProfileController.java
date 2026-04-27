package com.example.lecturesystem.modules.aiuserprofile.controller;

import com.example.lecturesystem.modules.aiuserprofile.service.AiUserProfileService;
import com.example.lecturesystem.modules.auth.security.LoginUser;
import com.example.lecturesystem.modules.permission.support.CurrentUserFacade;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/agent/user-profile")
public class AiUserProfileController {

    private final AiUserProfileService aiUserProfileService;
    private final CurrentUserFacade currentUserFacade;

    public AiUserProfileController(AiUserProfileService aiUserProfileService,
                                   CurrentUserFacade currentUserFacade) {
        this.aiUserProfileService = aiUserProfileService;
        this.currentUserFacade = currentUserFacade;
    }

    @GetMapping("/current")
    public Map<String, Object> current() {
        LoginUser user = currentUserFacade.currentLoginUser();
        return aiUserProfileService.queryCurrentProfile(user.getUserId());
    }

    @DeleteMapping("/item/{itemId}")
    public Map<String, Object> deleteItem(@PathVariable Long itemId) {
        LoginUser user = currentUserFacade.currentLoginUser();
        aiUserProfileService.deleteProfileItem(user.getUserId(), itemId);
        return Map.of("success", true);
    }

    @DeleteMapping("/clear")
    public Map<String, Object> clear() {
        LoginUser user = currentUserFacade.currentLoginUser();
        aiUserProfileService.clearUserProfile(user.getUserId());
        return Map.of("success", true);
    }
}