import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import Navbar from '../../components/common/Navbar';
import { getRfqs } from '../../api/rfqApi';
import { getQuotations } from '../../api/quotationApi';
import { Button } from '../../components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../../components/ui/card';
import { Badge } from '../../components/ui/badge';
import { FileText, Plus, DollarSign, Clock, TrendingUp, Package } from 'lucide-react';

const DashboardPage = () => {
  const { user } = useAuth();
  const [rfqs, setRfqs] = useState([]);
  const [quotations, setQuotations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const isBuyer = user?.role === 'buyer';
  const isVendor = user?.role === 'vendor';

  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      try {
        const rfqResponse = await getRfqs();
        setRfqs(rfqResponse.data.rfqs || []);

        if (isVendor) {
          const quotResponse = await getQuotations();
   setQuotations(quotResponse.data.quotations?.filter((q) => q.vendor?._id === user._id) || []);
        } else {
          // Buyers see quotations for their RFQs
          const quotResponse = await getQuotations();
          setQuotations(
  quotResponse.data.quotations?.filter((q) => q.rfq?.Buyer?._id === user._id) || []
);
        }
      } catch (err) {
        setError('Failed to load data');
        console.error(err);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, [isVendor, user]);

  const getStatusColor = (status) => {
    switch (status) {
      case 'open':
        return 'bg-green-100 text-green-800';
      case 'in_progress':
        return 'bg-blue-100 text-blue-800';
      case 'closed':
        return 'bg-gray-100 text-gray-800';
      case 'submitted':
        return 'bg-yellow-100 text-yellow-800';
      case 'under_review':
        return 'bg-blue-100 text-blue-800';
      case 'accepted':
        return 'bg-green-100 text-green-800';
      case 'rejected':
        return 'bg-red-100 text-red-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  if (loading) {
    return (
      <>
        <Navbar />
        <div className="min-h-screen bg-gray-50 flex items-center justify-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
        </div>
      </>
    );
  }

  return (
    <>
      <Navbar />
      <div className="min-h-screen bg-gray-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          <div className="mb-8">
            <h1 className="text-3xl font-bold text-gray-900">
              Welcome back, {user?.fullName?.split(' ')[0]}!
            </h1>
            <p className="text-gray-600 mt-2">
              {isBuyer
                ? 'Manage your RFQs and review quotations from vendors.'
                : 'Browse open RFQs and manage your quotations.'}
            </p>
          </div>
          {error && (
            <div className="mb-6 bg-red-50 border border-red-200 text-red-800 px-4 py-3 rounded">
              {error}
            </div>
          )}
          {isBuyer && (
            <>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
                <Card>
                  <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                    <CardTitle className="text-sm font-medium">Total RFQs</CardTitle>
                    <FileText className="h-4 w-4 text-gray-500" />
                  </CardHeader>
                  <CardContent>
                    <div className="text-2xl font-bold">{rfqs.length}</div>
                    <p className="text-xs text-gray-500 mt-1">Active requests for quotation</p>
                  </CardContent>
                </Card>
                <Card>
                  <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                    <CardTitle className="text-sm font-medium">Open RFQs</CardTitle>
                    <Package className="h-4 w-4 text-gray-500" />
                  </CardHeader>
                  <CardContent>
                    <div className="text-2xl font-bold">
                      {rfqs.filter((r) => r.status === 'open').length}
                    </div>
                    <p className="text-xs text-gray-500 mt-1">Accepting quotations</p>
                  </CardContent>
                </Card>
                <Card>
                  <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                    <CardTitle className="text-sm font-medium">In Progress</CardTitle>
                    <TrendingUp className="h-4 w-4 text-gray-500" />
                  </CardHeader>
                  <CardContent>
                    <div className="text-2xl font-bold">
                      {rfqs.filter((r) => r.status === 'in_progress').length}
                    </div>
                    <p className="text-xs text-gray-500 mt-1">Active contracts</p>
                  </CardContent>
                </Card>
              </div>
              <div className="flex justify-between items-center mb-6">
                <h2 className="text-2xl font-bold text-gray-900">Your RFQs</h2>
                <Link to="/rfqs/create">
                  <Button className="bg-blue-600 hover:bg-blue-700">
                    <Plus className="h-4 w-4 mr-2" />
                    Create New RFQ
                  </Button>
                </Link>
              </div>
              <div className="space-y-4">
                {rfqs.length === 0 ? (
                  <Card>
                    <CardContent className="py-12 text-center">
                      <FileText className="h-12 w-12 text-gray-400 mx-auto mb-4" />
                      <p className="text-gray-600 mb-4">You haven't created any RFQs yet.</p>
                      <Link to="/rfqs/create">
                        <Button className="bg-blue-600 hover:bg-blue-700">Create Your First RFQ</Button>
                      </Link>
                    </CardContent>
                  </Card>
                ) : (
                  rfqs
                    .filter((rfq) => rfq.Buyer?._id === user._id)
                    .map((rfq) => (
                      <Card key={rfq._id} className="hover:shadow-md transition-shadow">
                        <CardHeader>
                          <div className="flex justify-between items-start">
                            <div>
                              <CardTitle className="text-lg">{rfq.title}</CardTitle>
                              <CardDescription className="mt-1">
                                {rfq.description?.substring(0, 100)}...
                              </CardDescription>
                            </div>
                            <Badge className={getStatusColor(rfq.status)}>{rfq.status}</Badge>
                          </div>
                        </CardHeader>
                        <CardContent>
                          <div className="flex flex-wrap gap-4 text-sm text-gray-600 mb-4">
                            <div className="flex items-center">
                              <DollarSign className="h-4 w-4 mr-1" />
                              Budget: ₹{rfq.budget?.toLocaleString()}
                            </div>
                            <div className="flex items-center">
                              <Clock className="h-4 w-4 mr-1" />
                              Deadline: {new Date(rfq.deadline).toLocaleDateString()}
                            </div>
                            <div className="flex items-center">
                              <Package className="h-4 w-4 mr-1" />
                              {rfq.category}
                            </div>
                          </div>
                          <Link to={`/rfqs/${rfq._id}`}>
                            <Button variant="outline">View Details & Quotations</Button>
                          </Link>
                        </CardContent>
                      </Card>
                    ))
                )}
              </div>
              <div className="mt-8">
                <h2 className="text-2xl font-bold text-gray-900 mb-4">Received Quotations</h2>
                {quotations.length === 0 ? (
                  <Card>
                    <CardContent className="py-12 text-center">
                      <p className="text-gray-600">No quotations received yet.</p>
                    </CardContent>
                  </Card>
                ) : (
                  quotations.map((quotation) => (
                    <Card key={quotation._id} className="hover:shadow-md transition-shadow mb-4">
                      <CardHeader>
                        <div className="flex justify-between items-start">
                          <div>
                            <CardTitle className="text-lg">{quotation.rfq?.title}</CardTitle>
                            <CardDescription className="mt-1">
                              Vendor: {quotation.vendor?.companyName || quotation.vendor?.fullName}
                            </CardDescription>
                          </div>
                          <Badge className={getStatusColor(quotation.status)}>
                            {quotation.status}
                          </Badge>
                        </div>
                      </CardHeader>
                      <CardContent>
                        <div className="flex flex-wrap gap-4 text-sm text-gray-600 mb-4">
                          <div className="flex items-center">
                            <DollarSign className="h-4 w-4 mr-1" />
                            Price: ₹{quotation.price?.toLocaleString()}
                          </div>
                          <div className="flex items-center">
                            <Clock className="h-4 w-4 mr-1" />
                            Delivery: {quotation.deliveryTimeDays} days
                          </div>
                        </div>
                        <Link to={`/quotations/${quotation._id}`}>
                          <Button variant="outline">View Details</Button>
                        </Link>
                      </CardContent>
                    </Card>
                  ))
                )}
              </div>
            </>
          )}
          {isVendor && (
            <>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
                <Card>
                  <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                    <CardTitle className="text-sm font-medium">Open RFQs</CardTitle>
                    <FileText className="h-4 w-4 text-gray-500" />
                  </CardHeader>
                  <CardContent>
                    <div className="text-2xl font-bold">
                      {rfqs.filter((r) => r.status === 'open').length}
                    </div>
                    <p className="text-xs text-gray-500 mt-1">Available to bid on</p>
                  </CardContent>
                </Card>
                <Card>
                  <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                    <CardTitle className="text-sm font-medium">My Quotations</CardTitle>
                    <Package className="h-4 w-4 text-gray-500" />
                  </CardHeader>
                  <CardContent>
                    <div className="text-2xl font-bold">{quotations.length}</div>
                    <p className="text-xs text-gray-500 mt-1">Total submitted</p>
                  </CardContent>
                </Card>
                <Card>
                  <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                    <CardTitle className="text-sm font-medium">Accepted</CardTitle>
                    <TrendingUp className="h-4 w-4 text-gray-500" />
                  </CardHeader>
                  <CardContent>
                    <div className="text-2xl font-bold">
                      {quotations.filter((q) => q.status === 'accepted').length}
                    </div>
                    <p className="text-xs text-gray-500 mt-1">Quotations accepted</p>
                  </CardContent>
                </Card>
              </div>
              <div className="flex justify-between items-center mb-6">
                <h2 className="text-2xl font-bold text-gray-900">Open RFQs</h2>
                <Link to="/rfqs">
                  <Button variant="outline">View All RFQs</Button>
                </Link>
              </div>
              <div className="space-y-4">
                {rfqs.filter((r) => r.status === 'open').length === 0 ? (
                  <Card>
                    <CardContent className="py-12 text-center">
                      <FileText className="h-12 w-12 text-gray-400 mx-auto mb-4" />
                      <p className="text-gray-600">No open RFQs available.</p>
                    </CardContent>
                  </Card>
                ) : (
                  rfqs
                    .filter((r) => r.status === 'open')
                    .map((rfq) => (
                      <Card key={rfq._id} className="hover:shadow-md transition-shadow">
                        <CardHeader>
                          <div className="flex justify-between items-start">
                            <div>
                              <CardTitle className="text-lg">{rfq.title}</CardTitle>
                              <CardDescription className="mt-1">
                                {rfq.description?.substring(0, 100)}...
                              </CardDescription>
                            </div>
                            <Badge className={getStatusColor(rfq.status)}>{rfq.status}</Badge>
                          </div>
                        </CardHeader>
                        <CardContent>
                          <div className="flex flex-wrap gap-4 text-sm text-gray-600 mb-4">
                            <div className="flex items-center">
                              <DollarSign className="h-4 w-4 mr-1" />
                              Budget: ₹{rfq.budget?.toLocaleString()}
                            </div>
                            <div className="flex items-center">
                              <Clock className="h-4 w-4 mr-1" />
                              Deadline: {new Date(rfq.deadline).toLocaleDateString()}
                            </div>
                            <div className="flex items-center">
                              <Package className="h-4 w-4 mr-1" />
                              {rfq.category}
                            </div>
                          </div>
                          <div className="flex gap-2">
                            <Link to={`/rfqs/${rfq._id}`}>
                              <Button variant="outline">View Details</Button>
                            </Link>
                            <Link to={`/quotations/create/${rfq._id}`}>
                              <Button className="bg-green-600 hover:bg-green-700">
                                Submit Quotation
                              </Button>
                            </Link>
                          </div>
                        </CardContent>
                      </Card>
                    ))
                )}
              </div>
              <div className="mt-8">
                <h2 className="text-2xl font-bold text-gray-900 mb-4">My Quotations</h2>
                {quotations.length === 0 ? (
                  <Card>
                    <CardContent className="py-12 text-center">
                      <p className="text-gray-600">You haven't submitted any quotations yet.</p>
                    </CardContent>
                  </Card>
                ) : (
                  quotations.map((quotation) => (
                    <Card key={quotation._id} className="hover:shadow-md transition-shadow mb-4">
                      <CardHeader>
                        <div className="flex justify-between items-start">
                          <div>
                            <CardTitle className="text-lg">{quotation.rfq?.title}</CardTitle>
                            <CardDescription className="mt-1">
                              RFQ: {quotation.rfq?.title}
                            </CardDescription>
                          </div>
                          <Badge className={getStatusColor(quotation.status)}>
                            {quotation.status}
                          </Badge>
                        </div>
                      </CardHeader>
                      <CardContent>
                        <div className="flex flex-wrap gap-4 text-sm text-gray-600 mb-4">
                          <div className="flex items-center">
                            <DollarSign className="h-4 w-4 mr-1" />
                            Price: ₹{quotation.price?.toLocaleString()}
                          </div>
                          <div className="flex items-center">
                            <Clock className="h-4 w-4 mr-1" />
                            Delivery: {quotation.deliveryTimeDays} days
                          </div>
                        </div>
                        <Link to={`/quotations/${quotation._id}`}>
                          <Button variant="outline">View Details</Button>
                        </Link>
                      </CardContent>
                    </Card>
                  ))
                )}
              </div>
            </>
          )}
        </div>
      </div>
    </>
  );
};

export default DashboardPage;