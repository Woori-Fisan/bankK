import React, { useRef } from 'react';
import { User } from 'lucide-react';

interface RrnInputProps {
  rrnFront: string;
  rrnBack: string;
  onRrnFrontChange: (value: string) => void;
  onRrnBackChange: (value: string) => void;
  onBlur?: () => void;
  error?: string;
  label?: string;
  isChecking?: boolean;
}

const RrnInput: React.FC<RrnInputProps> = ({
  rrnFront,
  rrnBack,
  onRrnFrontChange,
  onRrnBackChange,
  onBlur,
  error,
  label = "본인 인증 (주민등록번호)",
  isChecking = false
}) => {
  const rrnFrontRef = useRef<HTMLInputElement>(null);
  const rrnBackRef = useRef<HTMLInputElement>(null);
  const [localError, setLocalError] = React.useState<string | undefined>(undefined);

  React.useEffect(() => {
    if (rrnFront.length === 6 && rrnBack.length === 1 && /^[1-4]$/.test(rrnBack)) {
      setLocalError(undefined);
    }
  }, [rrnFront, rrnBack]);

  const handleBlur = (e: React.FocusEvent<HTMLInputElement>) => {
    if (onBlur) {
      onBlur();
    }
    
    const currentFrontValue = rrnFrontRef.current?.value || '';
    const currentBackValue = rrnBackRef.current?.value || '';

    if (!currentFrontValue || currentFrontValue.length !== 6) {
      setLocalError('주민등록번호 앞 6자리를 입력해주세요.');
    } else if (e.relatedTarget === rrnBackRef.current && currentFrontValue.length === 6) {
      setLocalError(undefined);
    } else if (!currentBackValue || !/^[1-4]$/.test(currentBackValue)) {
      setLocalError('주민등록번호 뒤 1자리(1~4)를 입력해주세요.');
    } else {
      setLocalError(undefined);
    }
  };

  const handleFrontChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const val = e.target.value.replace(/[^0-9]/g, '');
    if (val.length <= 6) {
      onRrnFrontChange(val);
      if (val.length === 6) {
        rrnBackRef.current?.focus();
      }
    }
  };

  const handleBackChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const val = e.target.value.replace(/[^0-9]/g, '');
    if (val.length <= 1) {
      onRrnBackChange(val);
    }
  };

  const displayError = error || localError;

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between px-1">
        <label className="block text-sm font-black text-slate-700 uppercase tracking-widest flex items-center gap-2">
          <User className="w-4 h-4 text-emerald-500" />
          {label}
        </label>
        {isChecking && (
          <span className="text-xs text-emerald-600 font-bold animate-pulse bg-emerald-50 px-3 py-1 rounded-full border border-emerald-100">조회 중</span>
        )}
      </div>
      
      <div className="grid grid-cols-1 md:grid-cols-2 gap-8 items-end">
        <div className="space-y-2">
          <p className="text-[11px] font-bold text-slate-400 ml-1">주민번호 앞 6자리</p>
          <input
            ref={rrnFrontRef}
            type="text"
            inputMode="numeric"
            placeholder="000000"
            maxLength={6}
            value={rrnFront}
            onChange={handleFrontChange}
            onBlur={handleBlur}
            className="w-full px-5 py-4 bg-slate-50 border-2 border-slate-50 rounded-xl text-center text-slate-900 focus:bg-white focus:border-emerald-500 outline-none transition-all tracking-[0.3em] font-mono text-xl shadow-inner"
          />
        </div>
        
        <div className="space-y-2">
          <p className="text-[11px] font-bold text-slate-400 ml-1">주민번호 뒤 1자리</p>
          <div className="flex items-center gap-4">
            <div className="relative w-20">
              <input
                ref={rrnBackRef}
                type="password"
                inputMode="numeric"
                maxLength={1}
                value={rrnBack}
                onChange={handleBackChange}
                onBlur={handleBlur}
                className="w-full px-0 py-4 bg-slate-50 border-2 border-slate-50 rounded-xl text-center text-slate-900 focus:bg-white focus:border-emerald-500 outline-none transition-all font-mono text-xl shadow-inner"
              />
            </div>
            <div className="flex gap-2">
              {[...Array(6)].map((_, i) => (
                <div key={i} className="w-2.5 h-2.5 rounded-full bg-slate-200 shadow-inner"></div>
              ))}
            </div>
          </div>
        </div>
      </div>
      
      {displayError ? (
        <p className="mt-2 text-sm text-rose-500 font-bold ml-1 animate-in fade-in slide-in-from-top-1">{displayError}</p>
      ) : (
        <p className="mt-2 text-sm select-none pointer-events-none opacity-0">&nbsp;</p>
      )}
    </div>
  );
};

export default RrnInput;
