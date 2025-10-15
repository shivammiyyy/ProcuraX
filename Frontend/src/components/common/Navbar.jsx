import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { Button } from '../ui/button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '../ui/dropdown-menu';
import { ChevronDown, Menu, X, FileText, Home, Plus, ClipboardList } from 'lucide-react';
import { useState } from 'react';

const Navbar = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const isBuyer = user?.role === 'buyer';

  return (
    <nav className="bg-gradient-to-r from-blue-600 to-blue-500 shadow-md sticky top-0 z-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center h-16">
          {/* Logo */}
          <Link to="/" className="flex items-center space-x-2">
            <span className="text-2xl font-extrabold text-white tracking-tight">ProcuraX</span>
          </Link>

          {/* Desktop Menu */}
          <div className="hidden md:flex items-center space-x-4">
            <Link to="/">
              <Button variant="ghost" className="text-white hover:bg-white/10 flex items-center gap-2">
                <Home className="h-4 w-4" /> Home
              </Button>
            </Link>
            <Link to="/rfqs">
              <Button variant="ghost" className="text-white hover:bg-white/10 flex items-center gap-2">
                <FileText className="h-4 w-4" /> RFQs
              </Button>
            </Link>
            <Link to="/contracts">
              <Button variant="ghost" className="text-white hover:bg-white/10 flex items-center gap-2">
                <ClipboardList className="h-4 w-4" /> Contracts
              </Button>
            </Link>
            {isBuyer && (
              <Link to="/rfqs/create">
                <Button className="bg-white text-blue-600 hover:bg-blue-50 flex items-center gap-2">
                  <Plus className="h-4 w-4" /> Create RFQ
                </Button>
              </Link>
            )}
          </div>

          {/* Profile Dropdown */}
          <div className="hidden md:flex items-center">
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button variant="ghost" className="flex items-center gap-2 text-white hover:bg-white/10">
                  <div className="w-8 h-8 rounded-full bg-white text-blue-600 flex items-center justify-center font-semibold">
                    {user?.fullName?.charAt(0)?.toUpperCase() || 'U'}
                  </div>
                  <span className="font-medium">{user?.fullName?.split(' ')[0]}</span>
                  <ChevronDown className="h-4 w-4" />
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" className="w-56 shadow-md">
                <DropdownMenuItem className="flex flex-col items-start pb-2 border-b">
                  <div className="font-semibold text-gray-800">{user?.fullName}</div>
                  <div className="text-xs text-gray-500">{user?.email}</div>
                  <div className="text-xs text-blue-600 mt-1 capitalize">{user?.role}</div>
                </DropdownMenuItem>
                <DropdownMenuItem
                  onClick={handleLogout}
                  className="text-red-600 cursor-pointer hover:bg-red-50"
                >
                  Logout
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </div>

          {/* Mobile Menu Toggle */}
          <div className="md:hidden">
            <Button
              variant="ghost"
              size="icon"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              className="text-white hover:bg-white/10"
            >
              {mobileMenuOpen ? <X className="h-6 w-6" /> : <Menu className="h-6 w-6" />}
            </Button>
          </div>
        </div>
      </div>

      {/* Mobile Dropdown */}
      {mobileMenuOpen && (
        <div className="md:hidden bg-white border-t shadow-inner">
          <div className="px-4 py-3 space-y-2">
            <Link to="/" onClick={() => setMobileMenuOpen(false)}>
              <Button variant="ghost" className="w-full justify-start gap-2">
                <Home className="h-4 w-4" /> Home
              </Button>
            </Link>
            <Link to="/rfqs" onClick={() => setMobileMenuOpen(false)}>
              <Button variant="ghost" className="w-full justify-start gap-2">
                <FileText className="h-4 w-4" /> RFQs
              </Button>
            </Link>
            <Link to="/contracts" onClick={() => setMobileMenuOpen(false)}>
              <Button variant="ghost" className="w-full justify-start gap-2">
                <ClipboardList className="h-4 w-4" /> Contracts
              </Button>
            </Link>
            {isBuyer && (
              <Link to="/rfqs/create" onClick={() => setMobileMenuOpen(false)}>
                <Button variant="ghost" className="w-full justify-start gap-2">
                  <Plus className="h-4 w-4" /> Create RFQ
                </Button>
              </Link>
            )}
            <div className="border-t pt-2 mt-2">
              <div className="px-3 py-2">
                <div className="font-medium">{user?.fullName}</div>
                <div className="text-sm text-gray-500">{user?.email}</div>
                <div className="text-sm text-blue-600 capitalize">{user?.role}</div>
              </div>
              <Button
                variant="ghost"
                onClick={handleLogout}
                className="w-full justify-start text-red-600"
              >
                Logout
              </Button>
            </div>
          </div>
        </div>
      )}
    </nav>
  );
};

export default Navbar;
