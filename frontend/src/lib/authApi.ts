import { apiRequest } from "./apiClient";

// Mirrors authentication-service's LoginRequest/LoginResponse and
// user-profile-service's RegisterUserRequest/RegisterUserResponse -
// these are the Gateway-facing contracts, not internal service DTOs.

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  tokenType: string;
  expiresInMs: number;
}

export interface RegisterRequest {
  fullName: string;
  email: string;
  password: string;
  phone?: string;
}

export interface RegisterResponse {
  userId: string;
  fullName: string;
  email: string;
  phone: string;
  createdAt: string;
}

export interface ProfileResponse {
  userId: string;
  fullName: string;
  email: string;
  phone: string;
  createdAt: string;
  updatedAt: string;
}

export function login(request: LoginRequest): Promise<LoginResponse> {
  return apiRequest<LoginResponse>("/auth/login", { method: "POST", body: request, authenticated: false });
}

export function register(request: RegisterRequest): Promise<RegisterResponse> {
  return apiRequest<RegisterResponse>("/users/register", { method: "POST", body: request, authenticated: false });
}

export function getMyProfile(): Promise<ProfileResponse> {
  return apiRequest<ProfileResponse>("/users/me");
}

export function updateMyProfile(request: { fullName: string; phone: string }): Promise<ProfileResponse> {
  return apiRequest<ProfileResponse>("/users/me", { method: "PUT", body: request });
}
