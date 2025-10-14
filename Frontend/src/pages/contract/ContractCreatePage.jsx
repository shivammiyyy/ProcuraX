import React, { useEffect, useState } from 'react';
import Navbar from '../../components/common/Navbar';
import ContractForm from '../../components/contract/ContractForm';
import { useParams } from 'react-router-dom';
import { getQuotationById } from '../../api/quotationApi';
import { useAuth } from '../../context/AuthContext';

const ContractCreatePage = () => {
  const { quotationId } = useParams();
  const { user } = useAuth(); // ✅ buyer from auth context
  const [quotation, setQuotation] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // ✅ Fetch quotation details
  useEffect(() => {
    const fetchQuotation = async () => {
      try {
        const response = await getQuotationById(quotationId);
        setQuotation(response.data.quotation);
      } catch (err) {
        setError(err.response?.data?.message || 'Failed to load quotation details.');
      } finally {
        setLoading(false);
      }
    };
    fetchQuotation();
  }, [quotationId]);

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
