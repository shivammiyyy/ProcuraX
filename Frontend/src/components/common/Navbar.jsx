import React, { useState } from 'react';
import { useAuth } from '../../context/authContext';
import { Menu, X, ChevronDown } from 'lucide-react';


const Navbar = () => {
  const { user, logout } = useAuth();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [dropdownOpen, setDropdownOpen] = useState(false);

  const role = user?.role;
  const firstName = user?.fullName?.split(" ")[0] || "User";

  const handleLogout = () => {
    logout();
    setDropdownOpen(false);
  };

  return (
    <nav className="bg-white shadow-md sticky top-0 z-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between h-16 items-center">
          
          {/* Left: Logo */}
          <div className="flex items-center">
            <a href="/" className="text-2xl font-bold text-blue-600 tracking-wide">
              PROCURAX
            </a>
          </div>

          {/* Center: Nav links (hidden on mobile) */}
          <div className="hidden md:flex space-x-8">
            <a href="/" className="text-gray-700 hover:text-blue-600 transition">
              Home
            </a>
            <a href="/rfq" className="text-gray-700 hover:text-blue-600 transition">
              RFQ
            </a>
          </div>

          {/* Right: Actions */}
          <div className="flex items-center space-x-4">
            {role === 'buyer' && (
              <a
                href="/create-rfq"
                className="hidden sm:inline-block bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition"
              >
                Create RFQ
              </a>
            )}

            {/* User Menu */}
            <div className="relative">
              <button
                onClick={() => setDropdownOpen(!dropdownOpen)}
                className="flex items-center space-x-1 text-gray-800 hover:text-blue-600 font-medium focus:outline-none"
              >
                <span>{firstName}</span>
                <ChevronDown size={18} />
              </button>

              {/* Dropdown Menu */}
              {dropdownOpen && (
                <div className="absolute right-0 mt-2 w-40 bg-white border rounded-lg shadow-lg py-2 z-20">
                  <a
                    href="/profile"
                    className="block px-4 py-2 text-gray-700 hover:bg-gray-100"
                  >
                    Profile
                  </a>
                  <a
                    href="/settings"
                    className="block px-4 py-2 text-gray-700 hover:bg-gray-100"
                  >
                    Settings
                  </a>
                  <button
                    onClick={handleLogout}
                    className="w-full text-left px-4 py-2 text-red-600 hover:bg-gray-100"
                  >
                    Logout
                  </button>
                </div>
              )}
            </div>

            {/* Mobile Menu Button */}
            <button
              className="md:hidden text-gray-800 focus:outline-none"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
            >
              {mobileMenuOpen ? <X size={24} /> : <Menu size={24} />}
            </button>
          </div>
        </div>
      </div>

      {/* Mobile Menu Dropdown */}
      {mobileMenuOpen && (
        <div className="md:hidden bg-white border-t shadow-sm">
          <div className="flex flex-col space-y-2 px-4 py-3">
            <a href="/" className="text-gray-700 hover:text-blue-600 transition">
              Home
            </a>
            <a href="/rfq" className="text-gray-700 hover:text-blue-600 transition">
              RFQ
            </a>

            {role === 'buyer' && (
              <a
                href="/create-rfq"
                className="bg-blue-600 text-white px-4 py-2 rounded-lg text-center hover:bg-blue-700 transition"
              >
                Create RFQ
              </a>
            )}
          </div>
        </div>
      )}
    </nav>
  );
};

export default Navbar;
