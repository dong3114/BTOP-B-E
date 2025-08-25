package com.aiaca.btop.board.service;

import com.aiaca.btop.board.domain.BoardInfo;
import com.aiaca.btop.board.mapper.BoardMapper;
import com.aiaca.btop.member.service.MemberService;
import groovy.util.logging.Slf4j;
import lombok.RequiredArgsConstructor;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.logging.Logger;

@Service
@Slf4j
@RequiredArgsConstructor    // 얘는 final 선언된거 생성자의 파라미터로 받게함
public class BoardServicelmpl implements BoardService {
    private final BoardMapper boardMapper;
    private final MemberService memberService;

    @Override
    public List<BoardInfo> getBoardList() {
        return boardMapper.getBoardList();
    };
    @Override
    public List<BoardInfo> getBoardList(String memberNo) {
        return boardMapper.getBoardList(memberNo);
    }
    @Override
    public BoardInfo getBoardInfo(long boardNo){
        increaseViws(boardNo);
        return boardMapper.getBoardInfo(boardNo);
    }
    @Override
    public int getBoardCount(){
        return boardMapper.getBoardCount();
    }
    @Override
    public void insertBoardInfo(BoardInfo boardInfo) {
        boardMapper.insertBoardInfo(boardInfo);
    }
    @Override
    public void updateBoard(BoardInfo boardInfo){
        boardMapper.updateBoardInfo(boardInfo);
    }
    @Override
    public void deleteBoard(long boardNo){
        boardMapper.deleteBoardInfo(boardNo);
    }
    // 조회수 증가 내부 로직
    private void increaseViws(long boardNo) {
        boardMapper.incresaeViews(boardNo);
    }

}
