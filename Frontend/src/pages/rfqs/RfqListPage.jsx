import React, { useEffect, useState } from 'react';
import Navbar from '../../components/common/Navbar';
import RfqList from '../../components/rfq/RfqList';
import { getRfqs } from '../../api/rfqApi';

export default function RfqListPage() {
  const [rfqs, setRfqs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchRfqs = async () => {
      try {
        const response = await getRfqs();
        setRfqs(response?.data?.rfqs || []);
      } catch (err) {
        setError(err.response?.data?.message || 'Failed to load RFQs');
      } finally {
        setLoading(false);
      }
    };
    fetchRfqs();
  }, []);

  

  return (
  <div className="min-h-screen bg-slate-50 ">
      <Navbar />
  <div className="max-w-6xl mx-auto mt-8">
        <h1 className="text-3xl font-bold mb-6">All RFQs</h1>
        {loading && <p>Loading RFQs...</p>}
        {error && <p className="text-red-600">{error}</p>}
        {!loading && !error && <RfqList rfqs={rfqs} />}
      </div>
    </div>
  );
}