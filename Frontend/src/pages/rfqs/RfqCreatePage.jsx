import Navbar from '../../components/common/Navbar';
import RfqForm from '../../components/rfq/RfqForm';

export default function RfqCreatePage() {
  return (
        <div className="min-h-screen bg-slate-50 ">

      <Navbar />
  <div className="max-w-6xl mx-auto mt-8">
        <RfqForm />
      </div>
      </div>
  
  );
}
