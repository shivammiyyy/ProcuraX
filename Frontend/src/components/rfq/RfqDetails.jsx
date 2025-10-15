import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { getQuotationByRFQId } from "../../api/rfqApi";

const RfqDetails = ({ rfq }) => {
  const { user } = useAuth();
  const [quotations, setQuotations] = useState([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  const isRfqOwner = user?.role === "buyer" && rfq.Buyer?._id === user._id;

  useEffect(() => {
    if (!rfq?._id || !isRfqOwner) return;
    const fetch = async () => {
      try {
        const res = await getQuotationByRFQId(rfq._id);
        setQuotations(res?.data?.quotations || []);
      } catch (e) {
        console.error(e);
      } finally {
        setLoading(false);
      }
    };
    fetch();
  }, [rfq?._id, isRfqOwner]);

  return (
    <div className="max-w-5xl mx-auto bg-white border border-slate-200 rounded-xl shadow-md p-8">
      <h1 className="text-3xl font-bold text-slate-800 mb-3">{rfq.title}</h1>
      <p className="text-slate-500 mb-6">
        Posted by{" "}
        <span className="font-semibold text-indigo-600">
          {rfq.Buyer?.companyName || "Unknown Buyer"}
        </span>
      </p>

      <div className="grid sm:grid-cols-2 gap-4 text-slate-700 mb-6">
        <p><b>Description:</b> {rfq.description}</p>
        <p><b>Category:</b> {rfq.category}</p>
        <p><b>Budget:</b> ₹{rfq.budget?.toLocaleString()}</p>
        <p><b>Deadline:</b> {new Date(rfq.deadline).toLocaleDateString()}</p>
        <p><b>Status:</b> <span className="capitalize text-emerald-600">{rfq.status}</span></p>
      </div>

      {rfq.attachment?.length > 0 && (
        <div className="mb-8">
          <h3 className="text-xl font-semibold mb-2">Attachments</h3>
          <div className="flex flex-wrap gap-3">
            {rfq.attachment.map((f, i) => (
              <a
                key={i}
                href={f.filePath}
                target="_blank"
                rel="noopener noreferrer"
                className="px-3 py-2 bg-slate-100 rounded hover:bg-slate-200 text-indigo-600 text-sm"
              >
                {f.fileName}
              </a>
            ))}
          </div>
        </div>
      )}

      {isRfqOwner && (
        <div>
          <h3 className="text-2xl font-semibold mb-4">Submitted Quotations</h3>
          {loading ? (
            <p>Loading quotations...</p>
          ) : quotations.length === 0 ? (
            <p>No quotations yet.</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full border border-slate-200 rounded-lg text-sm">
                <thead className="bg-slate-100 text-slate-700">
                  <tr>
                    <th className="p-3 text-left">Vendor</th>
                    <th className="p-3 text-left">Price</th>
                    <th className="p-3 text-left">Delivery</th>
                    <th className="p-3 text-left">Status</th>
                    <th className="p-3 text-left">Action</th>
                  </tr>
                </thead>
                <tbody>
                  {quotations.map((q) => (
                    <tr key={q._id} className="border-t hover:bg-slate-50">
                      <td className="p-3">{q.vendor?.companyName}</td>
                      <td className="p-3">₹{q.price?.toLocaleString()}</td>
                      <td className="p-3">{q.deliveryTimeDays} days</td>
                      <td className="p-3 capitalize text-emerald-600">{q.status}</td>
                      <td className="p-3">
                        <button
                          onClick={() => navigate(`/quotations/${q._id}`)}
                          className="bg-indigo-600 text-white px-3 py-1 rounded hover:bg-indigo-700"
                        >
                          View
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default RfqDetails;
