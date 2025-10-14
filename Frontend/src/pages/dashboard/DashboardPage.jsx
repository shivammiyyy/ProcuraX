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
        const allRfqs = rfqResponse.data.rfqs || [];
        setRfqs(allRfqs);

        const quotResponse = await getQuotations();
        const allQuotations = quotResponse.data.quotations || [];

        if (isVendor) {
          // Vendor sees only their quotations
          setQuotations(allQuotations.filter(q => q.vendor?._id === user._id));
        } else if (isBuyer) {
          // Buyer sees quotations for their RFQs
          const myRfqIds = allRfqs.map(r => r._id);
          setQuotations(allQuotations.filter(q => myRfqIds.includes(q.rfq._id)));
        }
      } catch (err) {
        console.error(err);
        setError('Failed to load data');
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [isVendor, isBuyer, user]);

  const getStatusColor = (status) => {
    switch (status) {
      case 'open': return 'bg-green-100 text-green-800';
      case 'in_progress': return 'bg-blue-100 text-blue-800';
      case 'closed': return 'bg-gray-100 text-gray-800';
      case 'submitted': return 'bg-yellow-100 text-yellow-800';
      case 'under_review': return 'bg-blue-100 text-blue-800';
      case 'accepted': return 'bg-green-100 text-green-800';
      case 'rejected': return 'bg-red-100 text-red-800';
      default: return 'bg-gray-100 text-gray-800';
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

          {/* Buyer Dashboard */}
          {isBuyer && (
            <>
              {/* RFQ Stats */}
              <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
                <Card>
                  <CardHeader className="flex justify-between pb-2">
                    <CardTitle className="text-sm font-medium">Total RFQs</CardTitle>
                    <FileText className="h-4 w-4 text-gray-500" />
                  </CardHeader>
                  <CardContent>
                    <div className="text-2xl font-bold">{rfqs.length}</div>
                    <p className="text-xs text-gray-500 mt-1">Active requests for quotation</p>
                  </CardContent>
                </Card>
                <Card>
                  <CardHeader className="flex justify-between pb-2">
                    <CardTitle className="text-sm font-medium">Open RFQs</CardTitle>
                    <Package className="h-4 w-4 text-gray-500" />
                  </CardHeader>
                  <CardContent>
                    <div className="text-2xl font-bold">{rfqs.filter(r => r.status === 'open').length}</div>
                    <p className="text-xs text-gray-500 mt-1">Accepting quotations</p>
                  </CardContent>
                </Card>
                <Card>
                  <CardHeader className="flex justify-between pb-2">
                    <CardTitle className="text-sm font-medium">In Progress</CardTitle>
                    <TrendingUp className="h-4 w-4 text-gray-500" />
                  </CardHeader>
                  <CardContent>
                    <div className="text-2xl font-bold">{rfqs.filter(r => r.status === 'in_progress').length}</div>
                    <p className="text-xs text-gray-500 mt-1">Active contracts</p>
                  </CardContent>
                </Card>
              </div>

              {/* RFQ List */}
              <div className="flex justify-between items-center mb-6">
                <h2 className="text-2xl font-bold text-gray-900">Your RFQs</h2>
                <Link to="/rfqs/create">
                  <Button className="bg-blue-600 hover:bg-blue-700">
                    <Plus className="h-4 w-4 mr-2" /> Create New RFQ
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
                    .map(rfq => (
                      <Card key={rfq._id} className="hover:shadow-md transition-shadow">
                        <CardHeader>
                          <div className="flex justify-between items-start">
                            <div>
                              <CardTitle className="text-lg">{rfq.title}</CardTitle>
                              <CardDescription className="mt-1">{rfq.description?.substring(0, 100)}...</CardDescription>
                            </div>
                            <Badge className={getStatusColor(rfq.status)}>{rfq.status}</Badge>
                          </div>
                        </CardHeader>
                        <CardContent>
                          <div className="flex flex-wrap gap-4 text-sm text-gray-600 mb-4">
                            <div className="flex items-center"><DollarSign className="h-4 w-4 mr-1" /> Budget: ₹{rfq.budget?.toLocaleString()}</div>
                            <div className="flex items-center"><Clock className="h-4 w-4 mr-1" /> Deadline: {new Date(rfq.deadline).toLocaleDateString()}</div>
                            <div className="flex items-center"><Package className="h-4 w-4 mr-1" /> {rfq.category}</div>
                          </div>
                          <Link to={`/rfqs/${rfq._id}`}>
                            <Button variant="outline">View Details & Quotations</Button>
                          </Link>
                        </CardContent>
                      </Card>
                    ))
                )}
              </div>

              {/* Quotations */}
              <div className="mt-8">
                <h2 className="text-2xl font-bold text-gray-900 mb-4">Received Quotations</h2>
                {quotations.length === 0 ? (
                  <Card>
                    <CardContent className="py-12 text-center">
                      <p className="text-gray-600">No quotations received yet.</p>
                    </CardContent>
                  </Card>
                ) : (
                  quotations.map(q => (
                    <Card key={q._id} className="hover:shadow-md transition-shadow mb-4">
                      <CardHeader>
                        <div className="flex justify-between items-start">
                          <div>
                            <CardTitle className="text-lg">{q.rfq?.title}</CardTitle>
                            <CardDescription className="mt-1">Vendor: {q.vendor?.companyName || q.vendor?.fullName}</CardDescription>
                          </div>
                          <Badge className={getStatusColor(q.status)}>{q.status}</Badge>
                        </div>
                      </CardHeader>
                      <CardContent>
                        <div className="flex flex-wrap gap-4 text-sm text-gray-600 mb-4">
                          <div className="flex items-center"><DollarSign className="h-4 w-4 mr-1" /> Price: ₹{q.price?.toLocaleString()}</div>
                          <div className="flex items-center"><Clock className="h-4 w-4 mr-1" /> Delivery: {q.deliveryTimeDays} days</div>
                        </div>
                        <Link to={`/quotations/${q._id}`}>
                          <Button variant="outline">View Details</Button>
                        </Link>
                      </CardContent>
                    </Card>
                  ))
                )}
              </div>
            </>
          )}

          {/* Vendor Dashboard */}
          {isVendor && (
            <>
              {/* Vendor stats and RFQ/Quotation list */}
              {/* Keep your existing vendor cards and mapping logic as is */}
            </>
          )}
        </div>
      </div>
    </>
  );
};

export default DashboardPage;
