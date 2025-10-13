import Navbar from '../../components/common/Navbar';
import React, { useEffect, useState } from 'react';
import { getRfqs } from '../../api/rfqApi';
import RfqList from '../../components/rfq/RfqList';

export default function RfqListPage() {
  const [rfqs, setRfqs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchRfqs = async () => {
      try {
        const response = await getRfqs();
        setRfqs(response?.data?.rfqs || []); // ✅ fixed
      } catch (err) {
        setError(err.response?.data?.message || 'Failed to load RFQs');
      } finally {
        setLoading(false);
      }
    };
    fetchRfqs();
  }, []);

  return (
    <>
      <Navbar />
      <div className="p-8">
        {loading && <p>Loading RFQs...</p>}
        {error && <p className="text-red-600">{error}</p>}
        {!loading && !error && <RfqList rfqs={rfqs} />}
      </div>
    </>
  );
}
