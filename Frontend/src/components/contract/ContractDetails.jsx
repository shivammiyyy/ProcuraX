import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { getContractById } from '../../api/contractApi';
import { useAuth } from '../../context/AuthContext';

const ContractDetails = () => {
  const { id } = useParams();
  const { user } = useAuth();
  const [contract, setContract] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchContract = async () => {
      try {
        const response = await getContractById(id);
        setContract(response.contract);
      } catch (err) {
        setError(err.response?.data?.message || 'Failed to load contract details');
      } finally {
        setLoading(false);
      }
    };
    fetchContract();
  }, [id]);

  if (loading) return <p>Loading contract details...</p>;
  if (error) return <p className="text-red-600">{error}</p>;
  if (!contract) return <p>No contract found.</p>;

  // Restrict access to buyer or vendor involved in the contract
  const isAuthorized =
    (user.role === 'buyer' && contract.buyerId === user._id) ||
    (user.role === 'vendor' && contract.vendorId === user._id);

  if (!isAuthorized) {
    return <p className="text-red-600">You are not authorized to view this contract.</p>;
  }

  return (
    <div className="bg-white p-8 rounded shadow-md max-w-3xl mx-auto space-y-4">
      <h2 className="text-3xl font-bold">{contract.rfq?.title || 'Untitled Contract'}</h2>
      <p className="text-gray-700">{contract.rfq?.description}</p>
      <p className="font-semibold">
        Start Date: {contract.startDate ? new Date(contract.startDate).toLocaleDateString() : 'N/A'}
      </p>
      <p className="font-semibold">
        End Date: {contract.endDate ? new Date(contract.endDate).toLocaleDateString() : 'N/A'}
      </p>
      <p className="text-gray-500">
        Status: <span className="capitalize">{contract.status}</span>
      </p>
      {contract.contractFile?.filePath && (
        <a
          href={contract.contractFile.filePath}
          target="_blank"
          rel="noopener noreferrer"
          className="text-blue-600 hover:underline"
        >
          View Contract File
        </a>
      )}
      {contract.auditReport && (
        <div className="mt-4 p-4 bg-yellow-100 border-l-4 border-yellow-400 text-yellow-700">
          <h3 className="font-semibold mb-2">AI Contract Audit Report</h3>
          <pre className="whitespace-pre-wrap">{contract.auditReport}</pre>
        </div>
      )}
    </div>
  );
};

export default ContractDetails;