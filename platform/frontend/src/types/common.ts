/**
 * 공통 API 응답 구조
 * @template T 데이터 객체의 타입
 */
export interface ApiResponse<T = unknown> {
  /** 처리 결과 상태 ("SUCCESS", "ERROR") */
  status: 'SUCCESS' | 'ERROR';
  /** 비즈니스 상태/에러 코드 (예: 2000, 4000) */
  code: number;
  /** 처리 결과 메시지 (사용자 노출용) */
  message: string;
  /** 성공 시 반환되는 세부 데이터 객체 */
  data?: T;
}
