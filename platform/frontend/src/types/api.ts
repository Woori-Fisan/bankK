export interface ApiResponse<T> {
    success: boolean;
    data: T;
    error: any;
}
