import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getContracts } from '../../api/contractApi';
import { useAuth } from '../../context/AuthContext';

const ContractList = () => {
  const { user } = useAuth();
  const [contracts, setContracts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchContracts = async () => {
      try {
        const response = await getContracts();
        const allContracts = response.data?.contracts || [];

        // Filter contracts: buyers see their own, vendors see contracts where they are the vendor
        const filteredContracts = allContracts.filter((contract) => {
          if (!contract || !contract.buyer || !contract.vendor) return false;
          return (
            (user.role === 'buyer' && contract.buyer._id === user._id) ||
            (user.role === 'vendor' && contract.vendor._id === user._id)
          );
        });

        setContracts(filteredContracts);
      } catch (err) {
        setError(err.response?.data?.message || 'Failed to load contracts');
      } finally {
        setLoading(false);
      }
    };

    if (user?._id) fetchContracts();
  }, [user]);

  if (loading) return <p>Loading contracts...</p>;
  if (error) return <p className="text-red-600">{error}</p>;

  return (
    <div className="space-y-4">
      {contracts.length === 0 ? (
        <p>No contracts available.</p>
      ) : (
        contracts.map((contract) => (
          <div
            key={contract._id}
            className="bg-white p-4 rounded shadow flex justify-between items-center"
          >
            <div>
              <h2 className="text-xl font-semibold">
                {contract.rfq?.title || 'Untitled Contract'}
              </h2>
              <p className="text-gray-600">
                {contract.rfq?.description?.substring(0, 100) || 'No description'}
              </p>
              <p className="text-gray-500 text-sm">
                Status: <span className="capitalize">{contract.status || 'pending'}</span>
              </p>
            </div>
            <Link
              to={`/contracts/${contract._id}`}
              className="text-blue-600 hover:underline font-semibold"
            >
              View Details
            </Link>
          </div>
        ))
      )}
    </div>
  );
};

export default ContractList;
