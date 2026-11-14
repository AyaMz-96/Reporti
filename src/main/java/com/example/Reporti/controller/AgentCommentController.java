package com.example.Reporti.controller;

import com.example.Reporti.model.ReportComment;
import com.example.Reporti.service.AdminUserService;
import com.example.Reporti.service.AgentCommentService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/agents/me/reports/{reportId}/comments")
public class AgentCommentController {

    public record CommentRequest(String content) {}

    public record CommentResponse(Long id, Long reportId, String authorId,
                                  String authorName, String content, OffsetDateTime createdAt) {
        static CommentResponse from(ReportComment c) {
            return new CommentResponse(c.getId(), c.getReportId(), c.getAuthorId().toString(),
                    c.getAuthorName(), c.getContent(), c.getCreatedAt());
        }
    }

    private final AdminUserService userService;
    private final AgentCommentService commentService;

    public AgentCommentController(AdminUserService userService, AgentCommentService commentService) {
        this.userService = userService;
        this.commentService = commentService;
    }

    @GetMapping
    public List<CommentResponse> list(@PathVariable Long reportId,
                                      @RequestHeader("Authorization") String auth) {
        String agentId = userService.resolveAgentFromToken(auth).id();
        return commentService.list(reportId, agentId).stream().map(CommentResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse add(@PathVariable Long reportId,
                               @RequestBody CommentRequest request,
                               @RequestHeader("Authorization") String auth) {
        AdminUserService.AgentInfo me = userService.resolveAgentFromToken(auth);
        return CommentResponse.from(commentService.add(reportId, me.id(), me.fullName(), request.content()));
    }
}