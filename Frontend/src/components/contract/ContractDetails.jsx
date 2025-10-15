import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { getContractById, updateContract } from '../../api/contractApi';
import { useAuth } from '../../context/AuthContext';
import { motion } from 'framer-motion';

const ContractDetails = () => {
  const { id } = useParams();
  const { user } = useAuth();
  const [contract, setContract] = useState(null);
  const [loading, setLoading] = useState(true);
  const [updating, setUpdating] = useState(false);

  useEffect(() => {
    (async () => {
      try {
        const res = await getContractById(id);
        setContract(res.data.contract);
      } catch {
        alert('Failed to load contract');
      } finally {
        setLoading(false);
      }
    })();
  }, [id]);

  if (loading) return <p>Loading...</p>;
  if (!contract) return <p>No contract found.</p>;

  const isVendor = user.role === 'vendor' && contract.vendor._id === user._id;
  const formatDate = (d) => (d ? new Date(d).toLocaleDateString() : '—');

  const handleVendorAction = async (status) => {
    setUpdating(true);
    try {
      const updated = await updateContract(contract._id, { status });
      setContract(updated.data.contract);
    } catch {
      alert('Action failed');
    } finally {
      setUpdating(false);
    }
  };

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className="bg-white rounded-2xl shadow-lg p-8 border border-gray-200 space-y-6"
    >
      <header>
        <h2 className="text-3xl font-bold text-gray-800">
          {contract.rfq?.title || 'Contract'}
        </h2>
        <p className="text-gray-600 mt-1">{contract.rfq?.description}</p>
      </header>

      <section className="grid md:grid-cols-2 gap-6">
        <div className="bg-blue-50 p-4 rounded-xl border border-blue-100">
          <h3 className="font-semibold text-gray-800 mb-1">Buyer</h3>
          <p>{contract.buyer?.fullName}</p>
          <p className="text-sm text-gray-600">{contract.buyer?.companyName}</p>
        </div>
        <div className="bg-green-50 p-4 rounded-xl border border-green-100">
          <h3 className="font-semibold text-gray-800 mb-1">Vendor</h3>
          <p>{contract.vendor?.fullName}</p>
          <p className="text-sm text-gray-600">{contract.vendor?.companyName}</p>
        </div>
      </section>

      <section className="grid sm:grid-cols-3 gap-4">
        <div>
          <p className="text-gray-500 text-sm">Start Date</p>
          <p className="font-semibold">{formatDate(contract.startDate)}</p>
        </div>
        <div>
          <p className="text-gray-500 text-sm">End Date</p>
          <p className="font-semibold">{formatDate(contract.endDate)}</p>
        </div>
        <div>
          <p className="text-gray-500 text-sm">Status</p>
          <span
            className={`px-3 py-1 text-sm rounded-full font-semibold ${
              contract.status === 'Active'
                ? 'bg-green-100 text-green-700'
                : contract.status === 'Audited'
                ? 'bg-blue-100 text-blue-700'
                : 'bg-yellow-100 text-yellow-700'
            }`}
          >
            {contract.status}
          </span>
        </div>
      </section>

      {contract.content && (
        <section>
          <h3 className="font-semibold text-lg mb-2">Contract Content</h3>
          <pre className="whitespace-pre-wrap bg-gray-50 border rounded-lg p-4 text-gray-700">
            {contract.content}
          </pre>
        </section>
      )}

      {contract.contractFile && (
        <section>
          <a
            href={contract.contractFile.filePath}
            target="_blank"
            rel="noopener noreferrer"
            className="text-blue-600 hover:underline font-medium"
          >
            📄 Download {contract.contractFile.fileName}
          </a>
        </section>
      )}

      {isVendor && contract.status === 'Active' && (
        <div className="flex gap-4 pt-4">
          <button
            disabled={updating}
            onClick={() => handleVendorAction('Audited')}
            className="px-5 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700"
          >
            Accept
          </button>
          <button
            disabled={updating}
            onClick={() => handleVendorAction('Cancelled')}
            className="px-5 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700"
          >
            Cancel
          </button>
        </div>
      )}
    </motion.div>
  );
};

export default ContractDetails;
