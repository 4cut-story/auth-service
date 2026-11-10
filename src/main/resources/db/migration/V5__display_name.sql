-- 표시 이름(닉네임). 비어 있으면 화면은 로그인 아이디(nickname)를 쓴다. 주소(/@아이디)는 nickname 그대로다.
ALTER TABLE users ADD COLUMN display_name VARCHAR(30);
