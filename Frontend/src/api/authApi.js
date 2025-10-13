import axiosApi from "./axiosApi";

export const registerEmail = async (email) => {
  const response = await axiosApi.post('/auth/register', { email });
  return response.data;
}

export const verifyEmailAndSignup = async (email, otp, password, companyName, role, fullName) => {
  const response = await axiosApi.post('/auth/verify', { email, otp, password, companyName, role, fullName });
  return response.data;
}

export const loginUser = async (email, password) => {
  const response = await axiosApi.post('/auth/login', { email, password });
  return response.data;
}

export const getCurrentUser = async () => {
  const response = await axiosApi.get('/auth/getuser');
  return response.data;
}

