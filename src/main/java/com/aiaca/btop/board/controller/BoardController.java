package com.aiaca.btop.board.controller;

import com.aiaca.btop.board.domain.BoardInfo;
import com.aiaca.btop.board.mapper.BoardMapper;
import com.aiaca.btop.board.service.BoardService;
import com.aiaca.btop.member.domain.LoginInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/board")
public class BoardController {
    private final BoardService boardService;

    @GetMapping("/all")
    public ResponseEntity<?> allBoardList() {
        return ResponseEntity.ok(boardService.getBoardList());
    }

    @GetMapping("/{memberNo}")
    public ResponseEntity<?> getBoardInfo(@PathVariable("memberNo") String memberNo) {
        return ResponseEntity.ok(boardService.getBoardList(memberNo));
    }

    @GetMapping("/count")
    public ResponseEntity<?> getBoardCount() {
        return ResponseEntity.ok(boardService.getBoardCount());
    }
    @PostMapping("/add")
    public ResponseEntity<?> insertBoard(@Validated @RequestBody BoardInfo boardInfo) {
        //boardService.insertBoardInfo(boardInfo)
        return ResponseEntity.ok();
    }

    @PostMapping("/update")
    public ResponseEntity<?> updateBoard(@Validated @RequestBody BoardInfo boardInfo) {
        // boardService.updateBoard(boardInfo);
        return ResponseEntity.ok(" 게시물이 성공적으로 수정되었습니다. ")
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteBoard(@RequestParam("boardNo") long boardNo) {
        // boardService.deleteBoard(boardNo);
        return ResponseEntity.ok("게시물이 성공적으로 삭제되었습니다. ")
    }


}
