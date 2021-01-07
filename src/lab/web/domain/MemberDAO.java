package lab.web.domain;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;


public class MemberDAO {
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
	
	public void insert(MemberVO member) {
		Connection con = null;
		try {
			con = getConnection();
			String sql = "insert into member values(?,?,?,?,?)";
			PreparedStatement stmt = con.prepareStatement(sql);
			stmt.setString(1, member.getUserid());
			stmt.setString(2, member.getName());
			stmt.setString(3, member.getPassword());
			stmt.setString(4, member.getEmail());
			stmt.setString(5, member.getAddress());
			stmt.executeUpdate();
		} catch(SQLException e) {
			if(e.getMessage().contains("unique")) {
				throw new RuntimeException("아이디가 중복됩니다.");
			} else {
				e.printStackTrace();
				throw new RuntimeException("MemberDAO.insert() 예외발생 - 콘솔확인");
			}
		} finally {
			closeConnection(con);
		}
	}
	
	public MemberVO selectMember(String userid) {
		Connection con = null;
		MemberVO member = new MemberVO();
		try {
			con = getConnection();
			String sql = "select * from member where userid=?";
			PreparedStatement stmt = con.prepareStatement(sql);
			stmt.setString(1, userid);
			ResultSet rs = stmt.executeQuery();
			if(rs.next()) {
				member.setUserid(userid);
				member.setPassword(rs.getString("password"));
				member.setName(rs.getString("name"));
				member.setEmail(rs.getString("email"));
				member.setAddress(rs.getString("address"));
			}
		} catch(SQLException e) {
			e.printStackTrace();
			throw new RuntimeException("MemberDAO.selectMember() 예외발생 - 콘솔확인");
		} finally {
			closeConnection(con);
		}
		return member;
	}
	
	public void updateMember(MemberVO member) {
		Connection con = null;
		try {
			con = getConnection();
			String sql = "update member set email=?, address=?, name=?, password=? where userid=?";
			PreparedStatement stmt = con.prepareStatement(sql);
			stmt.setString(1, member.getEmail());
			stmt.setString(2, member.getAddress());
			stmt.setString(3, member.getName());
			stmt.setString(4, member.getPassword());
			stmt.setString(5, member.getUserid());
			stmt.executeUpdate();
		} catch(SQLException e) {
			e.printStackTrace();
			throw new RuntimeException("MemberDAO.update() 예외 발생 - 콘솔 확인");
		} finally {
			closeConnection(con);
		}
	}
	public String getPassword(String userid) {
		String pw = "";
		Connection con = null;
		try {
			con = getConnection();
			String sql = "select password from member where userid=?";
			PreparedStatement stmt = con.prepareStatement(sql);
			stmt.setString(1, userid);
			ResultSet rs = stmt.executeQuery();
			if(rs.next()) {
				pw = rs.getString("password");
			}
		} catch(SQLException e) {
			e.printStackTrace();
			throw new RuntimeException("MemberDAO.getPassword() 에러발생 - 콘솔확인");
		} finally {
			closeConnection(con);
		}
		return pw;
	}
	
	public void deleteMember(String userid, String password) {
		Connection con = null;
		String pw = "";
		try {
			con = getConnection();
			con.setAutoCommit(false);
			String sql = "select password from member where userid=?";
			PreparedStatement stmt = con.prepareStatement(sql);
			stmt.setString(1, userid);
			ResultSet rs = stmt.executeQuery();
			if(rs.next()) {
				pw = rs.getString("password");
			} else {
				throw new RuntimeException("아이디가 잘못 입력되었습니다.");
			}
			if(pw.equals(password)) {
				try {
					String sql2 = "delete from board where masterid in (select masterid from board where userid=?) and (replynumber>0 or userid=?)";
					stmt = con.prepareStatement(sql2);
					stmt.setString(1, userid); //왜 2번userid는 설정안ㅇ함?
					stmt.setString(2, userid);
					stmt.executeUpdate();
					String sql3 = "delete from member where userid=?";
					stmt = con.prepareStatement(sql3);
					stmt.setString(1, userid);
					stmt.executeUpdate();
					con.commit();
				} catch(SQLException e) {
					con.rollback();
					throw new RuntimeException("삭제가 되지 않았습니다. : " + e.getMessage());
				}
			} else {
				throw new RuntimeException("비밀번호가 다릅니다.");
			}
		} catch(SQLException e) {
			e.printStackTrace();
			throw new RuntimeException("MemberDAO.deleteMember() 예외발생 - 콘솔확인");
		} finally {
			closeConnection(con);
		}
	}

}
