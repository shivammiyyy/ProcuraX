import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getContracts } from '../../api/contractApi';

const ContractList = () => {
  const [contracts, setContracts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchContracts = async () => {
      try {
        const response = await getContracts();
        setContracts(response.contracts || []);
      } catch (err) {
        setError(err.response?.data?.message || 'Failed to load contracts');
      } finally {
        setLoading(false);
      }
    };
    fetchContracts();
  }, []);

  if (loading) return <p>Loading contracts...</p>;
  if (error) return <p className="text-red-600">{error}</p>;

  return (
    <div className="space-y-4">
      {contracts.map((contract) => (
        <div
          key={contract._id}
          className="bg-white p-4 rounded shadow flex justify-between items-center"
        >
          <div>
            <h2 className="text-xl font-semibold">{contract.rfq?.title || 'Untitled Contract'}</h2>
            <p className="text-gray-600">
              {contract.rfq?.description?.substring(0, 100)}
            </p>
          </div>
          <Link
            to={`/contracts/${contract._id}`}
            className="text-blue-600 hover:underline font-semibold"
          >
            View Details
          </Link>
        </div>
      ))}
      {contracts.length === 0 && <p>No contracts available.</p>}
    </div>
  );
};

export default ContractList;
