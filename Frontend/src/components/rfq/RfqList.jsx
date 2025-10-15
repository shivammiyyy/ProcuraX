import React from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";

const RfqList = ({ rfqs }) => {
  const { user } = useAuth();

  if (!rfqs.length)
    return <p className="text-center text-slate-500 mt-10">No RFQs available at the moment.</p>;

  return (
    <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-6">
      {rfqs.map((rfq) => (
        <div
          key={rfq._id}
          className="bg-white border border-slate-200 rounded-xl shadow-sm hover:shadow-md transition-all p-6 flex flex-col justify-between"
        >
          <div>
            <h2 className="text-xl font-bold text-slate-800 mb-2">{rfq.title}</h2>
            <p className="text-sm text-slate-600 mb-3 line-clamp-3">
              {rfq.description || "No description available."}
            </p>

            <div className="text-sm text-slate-500 space-y-1">
              <p>
                <span className="font-semibold">Category:</span> {rfq.category}
              </p>
              <p>
                <span className="font-semibold">Budget:</span> ₹{rfq.budget?.toLocaleString()}
              </p>
              <p>
                <span className="font-semibold">Deadline:</span>{" "}
                {new Date(rfq.deadline).toLocaleDateString()}
              </p>
              <p>
                <span className="font-semibold">Status:</span>{" "}
                <span className="capitalize text-emerald-600">{rfq.status}</span>
              </p>
            </div>
          </div>

          <div className="flex justify-between items-center mt-5">
            <Link
              to={`/rfqs/${rfq._id}`}
              className="text-indigo-600 font-semibold hover:underline"
            >
              View Details
            </Link>

            {user.role === "vendor" && rfq.status === "open" && (
              <Link
                to={`/quotations/create/${rfq._id}`}
                className="bg-indigo-600 text-white px-3 py-1 rounded hover:bg-indigo-700 text-sm"
              >
                Submit Quotation
              </Link>
            )}
          </div>
        </div>
      ))}
    </div>
  );
};

export default RfqList;
