import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getContracts } from '../../api/contractApi';
import { useAuth } from '../../context/AuthContext';
import { motion } from 'framer-motion';

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
        const filteredContracts = allContracts.filter((c) =>
          user.role === 'buyer' ? c.buyer._id === user._id : c.vendor._id === user._id
        );
        setContracts(filteredContracts);
      } catch (err) {
        setError(err.response?.data?.message || 'Failed to load contracts');
      } finally {
        setLoading(false);
      }
    };
    if (user?._id) fetchContracts();
  }, [user]);

  if (loading)
    return <p className="text-center text-gray-600">Loading contracts...</p>;
  if (error)
    return <p className="text-center text-red-600">{error}</p>;

  if (contracts.length === 0)
    return (
      <div className="text-center bg-white rounded-xl shadow-md p-10 border border-gray-200">
        <p className="text-gray-700 text-lg">
          No contracts found.{" "}
          <span className="text-blue-600 font-semibold">You’re all caught up!</span>
        </p>
      </div>
    );

  return (
    <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
      {contracts.map((contract) => (
        <motion.div
          key={contract._id}
          whileHover={{ scale: 1.02 }}
          className="bg-white rounded-xl shadow-md hover:shadow-xl border border-gray-200 transition-all p-6 flex flex-col justify-between"
        >
          <div>
            <h2 className="text-xl font-bold text-gray-800 mb-2">
              {contract.rfq?.title || 'Untitled Contract'}
            </h2>
            <p className="text-gray-600 text-sm mb-3 line-clamp-2">
              {contract.rfq?.description || 'No description provided.'}
            </p>
            <p className="text-sm text-gray-500">
              Status:{" "}
              <span
                className={`font-semibold capitalize ${
                  contract.status === 'Active'
                    ? 'text-green-600'
                    : contract.status === 'Cancelled'
                    ? 'text-red-600'
                    : 'text-yellow-600'
                }`}
              >
                {contract.status || 'pending'}
              </span>
            </p>
          </div>

          <Link
            to={`/contracts/${contract._id}`}
            className="mt-6 inline-block text-center bg-blue-600 text-white py-2 rounded-lg font-semibold hover:bg-blue-700 transition"
          >
            View Details
          </Link>
        </motion.div>
      ))}
    </div>
  );
};

export default ContractList;
