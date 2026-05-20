import React from 'react';
import { useEmployeeStore } from '../../../store/useEmployeeStore';
import { ChevronLeft, ChevronRight } from 'lucide-react';

const EmployeePagination: React.FC = () => {
    const { totalPages, currentPage, setPage } = useEmployeeStore();

    if (totalPages <= 1) return null;

    const handlePrev = () => {
        if (currentPage > 0) {
            setPage(currentPage - 1);
        }
    };

    const handleNext = () => {
        if (currentPage < totalPages - 1) {
            setPage(currentPage + 1);
        }
    };

    return (
        <div className="flex items-center justify-center gap-2 mt-6">
            <button
                onClick={handlePrev}
                disabled={currentPage === 0}
                className="p-2 rounded-full hover:bg-gray-100 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
                <ChevronLeft className="w-5 h-5 text-gray-600" />
            </button>
            <span className="text-sm font-medium text-gray-700">
                페이지 {currentPage + 1} / {totalPages}
            </span>
            <button
                onClick={handleNext}
                disabled={currentPage === totalPages - 1}
                className="p-2 rounded-full hover:bg-gray-100 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
                <ChevronRight className="w-5 h-5 text-gray-600" />
            </button>
        </div>
    );
};

export default EmployeePagination;
