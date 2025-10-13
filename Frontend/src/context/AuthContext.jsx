import { createContext, useState ,useContext, useEffect} from "react";
import * as authApi from '../api/authApi';


// Create Context
const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(() => {
    const storedUser = localStorage.getItem('user');
    return storedUser ? JSON.parse(storedUser) : null;
  });

  const [token, setToken] = useState(() => localStorage.getItem('token') || null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const isAuthenticated = !!token;

  // 🔹 Register user email to send OTP
  const registerEmail = async (email) => {
    try {
      setLoading(true);
      setError(null);
      const data = await authApi.registerEmail(email);
      return data; // returns { message, userId }
    } catch (err) {
      setError(err.response?.data?.message || 'Registration failed');
      throw err;
    } finally {
      setLoading(false);
    }
  };

  // 🔹 Verify OTP and complete signup
  const verifyAndSignup = async ({ email, otp, password, companyName, role, fullName }) => {
    try {
      setLoading(true);
      setError(null);
      const data = await authApi.verifyEmailAndSignup(email, otp, password, companyName, role, fullName);
      if (data.token && data.user) {
        setUser(data.user);
        setToken(data.token);
        localStorage.setItem('user', JSON.stringify(data.user));
        localStorage.setItem('token', data.token);
      }
      return data;
    } catch (err) {
      setError(err.response?.data?.message || 'OTP verification failed');
      throw err;
    } finally {
      setLoading(false);
    }
  };

  // 🔹 Login user
  const login = async (email, password) => {
    try {
      setLoading(true);
      setError(null);
      const data = await authApi.loginUser(email, password);
      if (data.token && data.user) {
        setUser(data.user);
        setToken(data.token);
        localStorage.setItem('user', JSON.stringify(data.user));
        localStorage.setItem('token', data.token);
      }
      return data;
    } catch (err) {
      setError(err.response?.data?.message || 'Login failed');
      throw err;
    } finally {
      setLoading(false);
    }
  };

  // 🔹 Get current user (validate token)
  const fetchCurrentUser = async () => {
    try {
      setLoading(true);
      const data = await authApi.getCurrentUser();
      setUser(data.user);
      return data.user;
    } catch (err) {
      console.error('Error fetching user:', err);
      logout(); // token invalid
    } finally {
      setLoading(false);
    }
  };

  // 🔹 Logout user
  const logout = () => {
    setUser(null);
    setToken(null);
    localStorage.removeItem('user');
    localStorage.removeItem('token');
  };

  // 🔹 Auto-fetch user when token exists
  useEffect(() => {
    if (token && !user) {
      fetchCurrentUser();
    }
  }, [token]);

  const value = {
    user,
    token,
    loading,
    error,
    isAuthenticated,
    registerEmail,
    verifyAndSignup,
    login,
    fetchCurrentUser,
    logout,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

// Custom Hook
export const useAuth = () => useContext(AuthContext);
