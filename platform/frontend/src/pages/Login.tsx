import { useState } from 'react';
import axios from 'axios';
import bcrypt from 'bcryptjs';

/**
 * BankBridge 플랫폼 로그인 테스트 (Login.tsx)
 * - 기능 확인을 위해 스타일을 최소화하고 구조를 간소화함
 */
function Login() {
  const [authStatus, setAuthStatus] = useState<{
  isLoading: boolean;
  error: string | null;
  data: unknown | null;
}>({
  isLoading: false,
  error: null,
  data: null,
});

  const handleLogin = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);
    
    // 1. 폼 내부의 데이터 추출 (name 속성 기준)
    const employeeId = formData.get('employeeId') as string;
    const plainPassword = formData.get('password') as string;

    setAuthStatus({ isLoading: true, error: null, data: null });

    try {
      // 2. 평문 비밀번호를 해시 암호화
      const hashedPassword = await bcrypt.hash(plainPassword, 10);
      
      // 3. 백엔드에 전송할 새로운 객체 조립 (평문 대신 해시값 장착!)
      const requestData = {
        employeeId: employeeId,
        password: hashedPassword,
      };

      // 4. 기존 Object.fromEntries(...) 대신 조립한 requestData를 전송
      const response = await axios.post('/auth/login', requestData);
      
      setAuthStatus({ isLoading: false, error: null, data: response.data });
    } catch (err) {
      // (이전 답변에서 적용했던 안전한 TypeScript 에러 처리 방식)
      let errorMessage = '로그인 중 오류가 발생했습니다.';
      if (axios.isAxiosError(err)) {
        errorMessage = err.response?.data?.message || err.message;
      } else if (err instanceof Error) {
        errorMessage = err.message;
      }

      setAuthStatus({ 
        isLoading: false, 
        error: errorMessage, 
        data: null 
      });
    }
  };

  const { isLoading, error, data } = authStatus;

  return (
    <div style={{ padding: '20px' }}>
      <h1>BankBridge 로그인 테스트</h1>

      <form onSubmit={handleLogin}>
        <div>
          <label>사번: </label>
          <input name="employeeId" type="text" required />
        </div>
        <br />
        <div>
          <label>비밀번호: </label>
          <input name="password" type="password" required />
        </div>
        <br />
        <button type="submit" disabled={isLoading}>
          {isLoading ? '로그인 중...' : '로그인'}
        </button>
      </form>

      <hr />

      {error && <p style={{ color: 'red' }}>에러: {error}</p>}

      {data !== null && (
        <div>
          <h3 style={{ color: 'green' }}>성공</h3>
          <pre>{JSON.stringify(data, null, 2)}</pre>
        </div>
      )}
    </div>
  );
}

export default Login;