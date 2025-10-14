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
        const response = await getQuotationById(quotationId);
        if (response.data.quotation.status !== 'accepted') {
          throw new Error('Quotation must be accepted to create a contract.');
        }
        if (user._id !== response.data.quotation.rfq?.Buyer?._id) {
          throw new Error('You are not authorized to create this contract.');
        }
        setQuotation(response.data.quotation);
      } catch (err) {
        setError(err.message || 'Failed to load quotation details.');
      } finally {
        setLoading(false);
      }
    };
    fetchQuotation();
  }, [quotationId, user]);

  if (loading) return <p className="text-center mt-10">Loading quotation details...</p>;
  if (error) return <p className="text-center text-red-600 mt-10">{error}</p>;

  return (
    <>
      <Navbar />
      <div className="p-8 min-h-screen bg-gray-50">
        <h1 className="text-3xl font-bold mb-6">Create Contract</h1>
        <ContractForm
          rfqId={quotation.rfq?._id}
          vendorId={quotation.vendor?._id}
          buyerId={user?._id}
          quotationId={quotation._id}
        />
      </div>
    </>
  );
};

export default ContractCreatePage;