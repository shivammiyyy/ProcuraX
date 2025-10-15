import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import Navbar from '../../components/common/Navbar';
import { getRfqs } from '../../api/rfqApi';
import { getQuotations } from '../../api/quotationApi';
import { Button } from '../../components/ui/button';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../../components/ui/card';
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
          // Vendor: only show their quotations
          setQuotations(allQuotations.filter(q => q.vendor?._id === user._id));
        } else if (isBuyer) {
          // Buyer: show quotations related to their RFQs
          const myRfqIds = allRfqs.map(r => r._id);
          setQuotations(allQuotations.filter(q => myRfqIds.includes(q.rfq?._id)));
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
      case 'accepted': return 'bg-green-200 text-green-800';
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
        <div className="max-w-7xl mx-auto px-6 py-10">
          <div className="mb-10">
            <h1 className="text-3xl font-bold text-gray-900">
              Welcome back, {user?.fullName?.split(' ')[0]} 👋
            </h1>
            <p className="text-gray-600 mt-2">
              {isBuyer
                ? 'Track your RFQs and manage vendor quotations seamlessly.'
                : 'Browse RFQs and manage your submitted quotations.'}
            </p>
          </div>

          {error && (
            <div className="mb-6 bg-red-50 border border-red-200 text-red-800 px-4 py-3 rounded">
              {error}
            </div>
          )}

          {/* ---------------- BUYER DASHBOARD ---------------- */}
          {isBuyer && (
            <>
              {/* Stat Cards */}
              <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-10">
                {[
                  { title: 'Total RFQs', value: rfqs.length, icon: FileText },
                  { title: 'Open RFQs', value: rfqs.filter(r => r.status === 'open').length, icon: Package },
                  { title: 'In Progress', value: rfqs.filter(r => r.status === 'in_progress').length, icon: TrendingUp },
                ].map((item, i) => (
                  <Card key={i} className="hover:shadow-md transition-shadow">
                    <CardHeader className="flex justify-between items-center pb-2">
                      <CardTitle className="text-sm font-medium text-gray-500">{item.title}</CardTitle>
                      <item.icon className="h-5 w-5 text-blue-500" />
                    </CardHeader>
                    <CardContent>
                      <div className="text-3xl font-semibold text-gray-900">{item.value}</div>
                    </CardContent>
                  </Card>
                ))}
              </div>

              {/* RFQs Section */}
              <div className="flex justify-between items-center mb-6">
                <h2 className="text-2xl font-semibold text-gray-900">Your RFQs</h2>
                <Link to="/rfqs/create">
                  <Button className="bg-blue-600 hover:bg-blue-700 text-white">
                    <Plus className="h-4 w-4 mr-2" /> New RFQ
                  </Button>
                </Link>
              </div>

              {rfqs.length === 0 ? (
                <Card className="py-12 text-center">
                  <FileText className="h-10 w-10 text-gray-400 mx-auto mb-4" />
                  <p className="text-gray-600">No RFQs yet. Create your first one!</p>
                </Card>
              ) : (
                <div className="space-y-4">
                  {rfqs.map(rfq => (
                    <Card key={rfq._id} className="hover:shadow-md transition-shadow border border-gray-100">
                      <CardHeader className="flex justify-between items-start">
                        <div>
                          <CardTitle className="text-lg font-semibold">{rfq.title}</CardTitle>
                          <CardDescription>{rfq.description?.slice(0, 100)}...</CardDescription>
                        </div>
                        <Badge className={getStatusColor(rfq.status)}>{rfq.status}</Badge>
                      </CardHeader>
                      <CardContent className="text-sm text-gray-600 flex flex-wrap gap-4">
                        <div>₹{rfq.budget?.toLocaleString()}</div>
                        <div><Clock className="inline h-4 w-4 mr-1" /> {new Date(rfq.deadline).toLocaleDateString()}</div>
                        <div><Package className="inline h-4 w-4 mr-1" /> {rfq.category}</div>
                        <div className="w-full mt-3">
                          <Link to={`/rfqs/${rfq._id}`}>
                            <Button variant="outline" size="sm">View Details</Button>
                          </Link>
                        </div>
                      </CardContent>
                    </Card>
                  ))}
                </div>
              )}
            </>
          )}

          {/* ---------------- VENDOR DASHBOARD ---------------- */}
          {isVendor && (
            <>
              {/* Available RFQs */}
              <div className="mb-10">
                <h2 className="text-2xl font-semibold text-gray-900 mb-4">Available RFQs</h2>
                {rfqs.filter(r => r.status === "open").length === 0 ? (
                  <Card className="py-12 text-center">
                    <p className="text-gray-600">No open RFQs available at the moment.</p>
                  </Card>
                ) : (
                  rfqs.filter(r => r.status === "open").map(rfq => (
                    <Card key={rfq._id} className="hover:shadow-md transition-shadow mb-4">
                      <CardHeader className="flex justify-between items-start">
                        <div>
                          <CardTitle className="text-lg font-semibold">{rfq.title}</CardTitle>
                          <CardDescription>{rfq.description?.substring(0, 100)}...</CardDescription>
                        </div>
                        <Badge className={getStatusColor(rfq.status)}>{rfq.status}</Badge>
                      </CardHeader>
                      <CardContent className="text-sm text-gray-600 flex flex-wrap gap-4">
                        <div>₹{rfq.budget?.toLocaleString()}</div>
                        <div><Clock className="inline h-4 w-4 mr-1" /> {new Date(rfq.deadline).toLocaleDateString()}</div>
                        <div><Package className="inline h-4 w-4 mr-1" /> {rfq.category}</div>
                        <div className="w-full mt-3">
                          <Link to={`/rfqs/${rfq._id}`}>
                            <Button variant="outline" size="sm">View Details</Button>
                          </Link>
                          <Link to={`/quotations/create/${rfq._id}`}>
                            <Button variant="outline" size="sm" className="ml-2">Create Quotation</Button>
                          </Link>
                        </div>
                      </CardContent>
                    </Card>
                  ))
                )}
              </div>

              {/* My Quotations */}
              <div>
                <h2 className="text-2xl font-semibold text-gray-900 mb-4">My Submitted Quotations</h2>
                {quotations.length === 0 ? (
                  <Card className="py-12 text-center">
                    <p className="text-gray-600">You haven’t submitted any quotations yet.</p>
                  </Card>
                ) : (
                  <div className="space-y-4">
                    {quotations.map(q => (
                      <Card key={q._id} className="hover:shadow-md transition-shadow border border-gray-100">
                        <CardHeader className="flex justify-between items-start">
                          <div>
                            <CardTitle className="text-lg font-semibold">
                              {q.rfq?.title || 'RFQ Not Found'}
                            </CardTitle>
                            <CardDescription>
                              {q.rfq?.description?.slice(0, 80)}...
                            </CardDescription>
                          </div>
                          <Badge className={getStatusColor(q.status)}>
                            {q.status || 'pending'}
                          </Badge>
                        </CardHeader>
                        <CardContent className="text-sm text-gray-600 flex flex-wrap gap-4">
                          <div><DollarSign className="inline h-4 w-4 mr-1" /> {q.price ? `₹${q.price}` : 'N/A'}</div>
                          <div><Clock className="inline h-4 w-4 mr-1" /> {new Date(q.createdAt).toLocaleDateString()}</div>
                          <div className="w-full mt-3">
                            <Link to={`/quotations/${q._id}`}>
                              <Button variant="outline" size="sm">View Details</Button>
                            </Link>
                          </div>
                        </CardContent>
                      </Card>
                    ))}
                  </div>
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
