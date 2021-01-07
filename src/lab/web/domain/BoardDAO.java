package lab.web.domain;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

public class BoardDAO {
	static{
		try {
			DriverManager.registerDriver(new oracle.jdbc.OracleDriver());
			System.out.println("드라이버 로드 완료");
		} catch(SQLException e) {
			e.printStackTrace();
			System.out.println("드라이버 로드 실패");
		}
	}// 초기화자({}), 드라이버 실행(1번만 실행되면 되므로 static을 붙힌다)
	
	private Connection getConnection() { // 커넥션 열기
		Connection con = null;
		try {
			Context ctx = new InitialContext(); // 컨테이너를 자바 객체로 형상화, 커낵션 풀이 이 안에 있음 (커넥션 풀 : 데이터베이스와 연결되는 커낵션의 수?)
			DataSource ds = (DataSource)ctx.lookup("java:comp/env/jdbc/Oracle");// 우리가 설정한 값 xml
			con = ds.getConnection();// ds안에 있는 커낵션을 하나 빼옴
		} catch(Exception e) {
			e.printStackTrace();
		}
		return con;
	}
	
	private void closeConnection(Connection con) { // 커넥션 닫기
		if(con!=null) {
			try {con.close();} catch(SQLException e) {}
		}
	}
	
	public void insertArticle(BoardVO board) {
		Connection con = null;
		String sql1 = "select nvl(max(bbsno),0) from board";
		int bbsno = 0;
		String sql2 = "insert into board(bbsno, userid, password, subject, content, writedate, masterid, readcount, replynumber, replystep) values(?,?,?,?,?,SYSDATE,?,0,0,0)";
		try {
			con = getConnection();
			PreparedStatement stmt = con.prepareStatement(sql1);
			ResultSet rs = stmt.executeQuery();
			rs.next();
			bbsno = rs.getInt(1) + 1;
			
			stmt = con.prepareStatement(sql2);
			stmt.setInt(1, bbsno);
			stmt.setString(2, board.getUserId());
			stmt.setString(3, board.getPassword());
			stmt.setString(4, board.getSubject());
			stmt.setString(5, board.getContent());
			stmt.setInt(6, bbsno);
			stmt.executeUpdate();
		} catch(SQLException e) {
			e.printStackTrace();
			throw new RuntimeException("BoardDAO.insertArticle 예외발생 - 콘솔확인");
		} finally {
			closeConnection(con);
		}
	}
	
	public Collection<BoardVO> selectArticleList(int page){
		Connection con = null;
		ArrayList<BoardVO> list = new ArrayList<>();
		String sql = "select bbsno, name, subject, writedate, readcount, rnum from ("
				+ "select bbsno, name, subject, writedate, readcount, rownum as rnum from ("
				+ "select bbsno, name, subject, writedate, readcount from board b "
				+ "join member m on b.userid=m.userid "
				+ "order by masterid desc, replynumber, replystep)) "
				+ "where rnum between ? and ?";
		int start = (page-1)*10+1;
		int end = start + 9;
		try {
			con = getConnection();
			PreparedStatement stmt = con.prepareStatement(sql);
			stmt.setInt(1, start);
			stmt.setInt(2, end);
			ResultSet rs = stmt.executeQuery();
			while(rs.next()) {
				BoardVO board = new BoardVO();
				board.setBbsno(rs.getInt("bbsno"));
				board.setName(rs.getString("name"));
				board.setSubject(rs.getString("subject"));
				board.setWriteDate(rs.getDate("writedate"));
				board.setReadCount(rs.getInt("readcount"));
				list.add(board);
			}
		}catch(SQLException e) {
			e.printStackTrace();
			throw new RuntimeException("BoardDAO.selectArticleList() 예외발생 - 콘솔확인");
		} finally {
			closeConnection(con);
		}
		return list;
	}
	
	public BoardVO selectArticle(int bbsno) {
		Connection con = null;
		BoardVO board = null;
		String sql = "select bbsno, name, b.userid, subject, content, readcount, "
				+ "writedate, masterid, replynumber, replystep "
				+ "from board b join member m on b.userid=m.userid where bbsno=?";
		try {
			con = getConnection();
			PreparedStatement stmt = con.prepareStatement(sql);
			stmt.setInt(1, bbsno);
			ResultSet rs = stmt.executeQuery();
			if(rs.next()) {
				board = new BoardVO();
				board.setBbsno(rs.getInt("bbsno"));
				board.setName(rs.getString("name"));
				board.setUserId(rs.getString("userid"));
				board.setSubject(rs.getString("subject"));
				board.setContent(rs.getString("content"));
				board.setReadCount(rs.getInt("readcount"));
				board.setWriteDate(rs.getDate("writedate"));
				board.setMasterId(rs.getInt("masterid"));
				board.setReplyNumber(rs.getInt("replynumber"));
				board.setReplyStep(rs.getInt("replystep"));
			}
		} catch(SQLException e) {
			e.printStackTrace();
			throw new RuntimeException("BoardDAO.selectArticle() 예외발생 - 콘솔확인");
		} finally {
			closeConnection(con);
		}
		return board;
	}
	public void updateReadCount(int bbsno) {
		Connection con = null;
		String sql = "update board set readcount=readcount+1 where bbsno=?";
		try {
			con = getConnection();
			PreparedStatement stmt = con.prepareStatement(sql);
			stmt.setInt(1, bbsno);
			stmt.executeUpdate();
		} catch(SQLException e) {
			e.printStackTrace();
			throw new RuntimeException("MemberDAO.updateReadCount() 예외발생 - 콘솔확인");
		} finally {
			closeConnection(con);
		}
	}
	public String getPassword(int bbsno) {
		Connection con = null;
		String password = "";
		String sql = "select password from board where bbsno=?";
		try {
			con = getConnection();
			PreparedStatement stmt = con.prepareStatement(sql);
			stmt.setInt(1, bbsno);
			ResultSet rs = stmt.executeQuery();
			while(rs.next()) {
				password = rs.getString("password");
			}
		}catch(SQLException e) {
			e.printStackTrace();
			throw new RuntimeException("BoardDAO.getPassword() 예외발생 - 콘솔확인");
		} finally {
			closeConnection(con);
		}
		return password;
	}
	
	public void replyArticle(BoardVO board) {
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			con = getConnection();
			con.setAutoCommit(false);
			
			String sql = "update board set replynumber=replynumber+1 where masterid=? and replynumber>?";
			stmt = con.prepareStatement(sql);
			stmt.setInt(1, board.getMasterId());
			stmt.setInt(2, board.getReplyNumber());
			stmt.executeUpdate();
			
			String sql2 = "select max(bbsno) from board";
			stmt = con.prepareStatement(sql2);
			rs = stmt.executeQuery();
			if(rs.next()) {
				board.setBbsno(rs.getInt(1)+1);
			}
			String sql3 = "insert into board values(?,?,?,?,?,SYSDATE,?,0,?,?)";
			stmt = con.prepareStatement(sql3);
			stmt.setInt(1, board.getBbsno());
			stmt.setString(2, board.getUserId());
			stmt.setString(3, board.getPassword());
			stmt.setString(4, board.getSubject());
			stmt.setString(5, board.getContent());
			stmt.setInt(6, board.getMasterId());
			stmt.setInt(7, board.getReplyNumber()+1);
			stmt.setInt(8, board.getReplyStep()+1);
			stmt.executeUpdate();
			con.commit();
		} catch(Exception e) {
			try {
				con.rollback();
			} catch (SQLException e1) {} 
			e.printStackTrace();
			throw new RuntimeException("BoardDAO.replyArticle() 예외발생 - 콘솔확인");
		}finally {
			closeConnection(con);
		}
	}
	
	public void deleteArticle(int bbsno, int replynumber) {
		String sql = "";
		Connection con = null;
		try {
			con = getConnection();
			if(replynumber>0) {
				sql = "delete from board where bbsno=?";
			} else {
				sql = "delete from board where masterid=?";
			}
			PreparedStatement stmt = con.prepareStatement(sql);
			stmt.setInt(1, bbsno);
			stmt.executeUpdate();
		} catch(SQLException e) {
			e.printStackTrace();
			throw new RuntimeException("BoardDAO.deleteArticle() 예외발생 - 콘솔확인");
		} finally {
			closeConnection(con);
		}
	}
	public int selectTotalBbsCount() {
		Connection con = null;
		String sql = "select count(bbsno) from board";
		try {
			con = getConnection();
			PreparedStatement stmt = con.prepareStatement(sql);
			ResultSet rs = stmt.executeQuery();
			rs.next();
			int bbsCount = rs.getInt(1);
			return bbsCount;
		} catch(SQLException e) {
			e.printStackTrace();
			throw new RuntimeException("BoardDAO.selectTotalBbsCount() 예외발생 - 콘솔확인");
		} finally {
			closeConnection(con);
		}
	}
	public void updateArticle(BoardVO board) {
		Connection con = null;
		String sql = "update board set subject=?, content=?, writedate=SYSDATE where bbsno=?";
		try {
			con = getConnection();
			PreparedStatement stmt = con.prepareStatement(sql);
			stmt.setString(1, board.getSubject());
			stmt.setString(2, board.getContent());
			stmt.setInt(3, board.getBbsno());
			stmt.executeUpdate();
		}catch(SQLException e) {
			e.printStackTrace();
			throw new RuntimeException("BoardDAO.updateArticle() 예외발생 - 콘솔확인");
		} finally {
			closeConnection(con);
		}
	}
	public int selectCount(String userid) {
		Connection con = null;
		String sql = "select count(bbsno) from board where userid=?";
		try {
			con = getConnection();
			PreparedStatement stmt = con.prepareStatement(sql);
			stmt.setString(1, userid);
			ResultSet rs = stmt.executeQuery();
			rs.next();
			int count = rs.getInt(1);
			return count;
		}catch(SQLException e) {
			e.printStackTrace();
			throw new RuntimeException("BoardDAO.selectCount() 예외발생 - 콘솔확인");
		} finally {
			closeConnection(con);
		}
	}
	public Collection<BoardVO> memberList(String userid, int page){
		Connection con = null;
		String sql = "select rnum, bbsno, name, subject, readcount, writedate from "
				+ "(select rownum rnum, bbsno, name, subject, readcount, writedate from "
				+ "(select bbsno, name, subject, readcount, writedate from board b "
				+ "join member m on b.userid=m.userid where b.userid=? order by bbsno desc)) "
				+ "where rnum between ? and ?";
		ArrayList<BoardVO> list = new ArrayList<>();
		int start = (page-1)*20+1;
		int end = start+19;
		try {
			con = getConnection();
			PreparedStatement stmt = con.prepareStatement(sql);
			stmt.setString(1, userid);
			stmt.setInt(2, start);
			stmt.setInt(3, end);
			ResultSet rs = stmt.executeQuery();
			while(rs.next()) {
				BoardVO board = new BoardVO();
				board.setBbsno(rs.getInt("bbsno"));
				board.setName(rs.getString("name"));
				board.setWriteDate(rs.getDate("writedate"));
				board.setSubject(rs.getString("subject"));
				board.setReadCount(rs.getInt("readcount"));
				list.add(board);
			}
		} catch(SQLException e) {
			e.printStackTrace();
			throw new RuntimeException("BoardDAO.memberList() 예외발생 - 콘솔확인");
		} finally {
			closeConnection(con);
		}
		return list;
	}

}
