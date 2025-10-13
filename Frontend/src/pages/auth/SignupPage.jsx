import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import RegisterForm from '../../components/auth/RegisterForm';
import VerifyEmailForm from '../../components/auth/VerifyEmailForm';
import { useAuth } from '../../context/AuthContext';

const SignupPage = () => {
  const [email, setEmail] = useState('');
  const [otpSent, setOtpSent] = useState(false);
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    if (isAuthenticated) {
      navigate('/dashboard');
    }
  }, [isAuthenticated, navigate]);

  return (
    <div className="min-h-screen flex flex-col md:flex-row bg-gray-50">
      <div className="hidden md:flex flex-1 bg-blue-600 text-white flex-col justify-center items-center p-12">
        <h1 className="text-4xl font-bold mb-4">Welcome to ProcuraX</h1>
        <p className="text-lg text-blue-100 text-center max-w-md">
          Connect buyers and vendors seamlessly. Create RFQs, manage quotes, and grow your business efficiently.
        </p>
      </div>

      <div className="flex-1 flex items-center justify-center px-6 py-12">
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
      </div>
    </div>
  );
};

export default SignupPage;
