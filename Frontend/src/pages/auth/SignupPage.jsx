import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import RegisterForm from '../../components/auth/RegisterForm';
import VerifyEmailForm from '../../components/auth/VerifyEmailForm';

const SignupPage = () => {
  const [email, setEmail] = useState('');
  const [otpSent, setOtpSent] = useState(false);
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    if (isAuthenticated) navigate('/dashboard');
  }, [isAuthenticated, navigate]);

  return (
    <div
      className="relative min-h-screen flex items-center justify-center bg-cover bg-center"
      style={{
        backgroundImage: `url('https://images.unsplash.com/photo-1522071820081-009f0129c71c?auto=format&fit=crop&w=1400&q=80')`,
      }}
    >
      {/* Overlay */}
      <div className="absolute inset-0 bg-black/50 backdrop-blur-sm"></div>

      {/* Main Content */}
      <motion.div
        initial={{ opacity: 0, y: 40 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.6 }}
        className="relative z-10 flex flex-col md:flex-row items-center justify-center gap-12 px-6"
      >
        {/* Left Section */}
        <div className="hidden md:flex flex-col text-white max-w-md space-y-4">
          <h1 className="text-5xl font-extrabold leading-tight drop-shadow-lg">
            Join <span className="text-blue-400">ProcuraX</span> Today
          </h1>
          <p className="text-lg text-gray-200">
            Simplify procurement. Connect with trusted vendors and streamline your business workflows effortlessly.
          </p>
        </div>

        {/* Right Section (Glassmorphism Form) */}
        <motion.div
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: 0.5, delay: 0.2 }}
          className="bg-white/90 backdrop-blur-lg rounded-2xl shadow-2xl p-6 w-full max-w-md border border-white/30"
        >
          {!otpSent ? (
            <RegisterForm
              onSuccess={(enteredEmail) => {
                setEmail(enteredEmail);
                setOtpSent(true);
              }}
            />
          ) : (
            <VerifyEmailForm email={email} />
          )}

          {/* ✅ Sign In Redirect */}
          <p className="text-center text-sm text-gray-700 mt-4">
            Already have an account?{' '}
            <button
              onClick={() => navigate('/login')}
              className="text-blue-600 font-medium hover:underline"
            >
              Sign In
            </button>
          </p>
        </motion.div>
      </motion.div>
    </div>
  );
};

export default SignupPage;
