import React, { useEffect, useState } from 'react';
import Navbar from '../../components/common/Navbar';
import ContractForm from '../../components/contract/ContractForm';
import { useParams } from 'react-router-dom';
import { getQuotationById } from '../../api/quotationApi';
import { useAuth } from '../../context/AuthContext';

const ContractCreatePage = () => {
  const { quotationId } = useParams();
  const { user } = useAuth();
  const [quotation, setQuotation] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchQuotation = async () => {
      try {
        const res = await getQuotationById(quotationId);
        if (res.data.quotation.status !== 'accepted')
          throw new Error('Quotation must be accepted to create a contract.');
        setQuotation(res.data.quotation);
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };
    fetchQuotation();
  }, [quotationId]);

  return (
    <>
      <Navbar />
      <div className="min-h-screen bg-gray-50 py-10 px-6">
        <div className="max-w-3xl mx-auto">
          <h1 className="text-4xl font-bold text-gray-800 mb-8">Create New Contract</h1>
          {loading ? (
            <p>Loading quotation...</p>
          ) : error ? (
            <p className="text-red-600">{error}</p>
          ) : (
            <ContractForm
              rfqId={quotation.rfq?._id}
              vendorId={quotation.vendor?._id}
              buyerId={user?._id}
              quotationId={quotation._id}
            />
          )}
        </div>
      </div>
    </>
  );
};

export default ContractCreatePage;
