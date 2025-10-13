import React, { useState } from 'react';
import RegisterForm from '../../components/auth/RegisterForm';
import VerifyEmailForm from '../../components/auth/VerifyEmailForm';

const Signup = () => {
  const [email, setEmail] = useState('');
  const [otpSent, setOtpSent] = useState(false);

  return (
    <div className="min-h-screen flex flex-col md:flex-row bg-gray-50">
      {/* Left Section */}
      <div className="hidden md:flex flex-1 bg-indigo-600 text-white flex-col justify-center items-center p-12">
        <h1 className="text-4xl font-bold mb-4">Welcome to ProcuraX</h1>
        <p className="text-lg text-indigo-100 text-center max-w-md">
          Connect buyers and vendors seamlessly. Create RFQs, manage quotes, and grow your business efficiently.
        </p>
        <img
          src="/assets/auth-illustration.svg"
          alt="ProcuraX Illustration"
          className="w-3/4 mt-10"
        />
      </div>

      {/* Right Section */}
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

export default Signup;
