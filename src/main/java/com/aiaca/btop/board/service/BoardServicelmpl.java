package com.aiaca.btop.board.service;

import com.aiaca.btop.board.domain.BoardInfo;
import com.aiaca.btop.board.mapper.BoardMapper;
import com.aiaca.btop.member.service.MemberService;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public class BoardServicelmpl implements MemberService {
    private static final Logger logger = LoggerFactory.getLogger(BoardServicelmpl.class);
    private final BoardMapper boardMapper;

    @Override
    public List<BoardInfo> getBoardList() {
        return boardMapper.getBoardList();
    };
    @Override
    public <BoardInfo> getBoardList(String memberNo) {
        return boardMapper.getBoardList(memberNo);
    }
    @Override
    public getBoardInfo(long boardNo){
        boardMapper.incresaeViews(boardNo);
        return boardMapper.getBoardInfo(boardNo)
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
    public void increaseViws(long boardNo) {
        boardMapper.incresaeViews(boardNo);
    }

}
