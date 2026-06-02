import React, { useState, useEffect } from 'react';
import { ShieldCheck, ShieldAlert } from 'lucide-react';
import { importPKCS8 } from 'jose';
import { saveTerminalPrivateKey, getTerminalPrivateKey } from '../../utils/terminalKeyStore';

const SecurityStatus: React.FC = () => {
    const [hasKey, setHasKey] = useState<boolean>(false);
    const [isRegistering, setIsRegistering] = useState<boolean>(false);
    const [keyInput, setKeyInput] = useState<string>('');
    const [isChecking, setIsChecking] = useState<boolean>(true);

    useEffect(() => {
        const checkKeyStatus = async () => {
            try {
                const key = await getTerminalPrivateKey();
                setHasKey(!!key);
            } catch (error) {
                console.error("키 확인 중 오류 발생:", error);
                setHasKey(false);
            } finally {
                setIsChecking(false);
            }
        };
        checkKeyStatus();
    }, []);

    const handleRegister = async () => {
        if (!keyInput.trim()) {
            alert('개인키를 입력해주세요.');
            return;
        }
        
        try {
            // PEM 문자열을 CryptoKey 객체로 파싱 (RS256 서명용)
            // 주의: 브라우저 Web Crypto API에서 사용할 수 있는 형태로 변환합니다.
            const cryptoKey = await importPKCS8(keyInput.trim(), 'RS256') as CryptoKey;
            
            // IndexedDB에 CryptoKey 객체 자체를 저장
            await saveTerminalPrivateKey(cryptoKey);
            
            setHasKey(true);
            setIsRegistering(false);
        } catch (error) {
            console.error("키 파싱/저장 실패:", error);
            alert('유효하지 않은 개인키 형식입니다. (BEGIN PRIVATE KEY 로 시작하는 PEM인지 확인)');
        }
    };

    if (isChecking) {
        return (
            <div className="w-full mt-8 bg-slate-50 rounded-lg p-4 flex items-center justify-center gap-2 border border-slate-100">
                <span className="text-xs font-medium text-slate-500">
                    보안 환경 검증 중...
                </span>
            </div>
        );
    }

    if (isRegistering) {
        return (
            <div className="w-full mt-8 bg-orange-50 rounded-lg p-4 flex flex-col gap-2 border border-orange-100">
                <span className="text-xs font-medium text-orange-800 text-center">
                    발급받은 단말기 개인키를 입력하세요
                </span>
                <textarea 
                    className="w-full text-xs p-2 border rounded" 
                    rows={4} 
                    value={keyInput}
                    onChange={(e) => setKeyInput(e.target.value)}
                    placeholder="-----BEGIN PRIVATE KEY-----..."
                />
                <div className="flex gap-2 justify-end mt-2">
                    <button 
                        className="text-xs px-3 py-1 bg-slate-200 hover:bg-slate-300 rounded text-slate-700 transition-colors"
                        onClick={() => setIsRegistering(false)}
                    >
                        취소
                    </button>
                    <button 
                        className="text-xs px-3 py-1 bg-orange-500 hover:bg-orange-600 text-white rounded transition-colors"
                        onClick={handleRegister}
                    >
                        저장
                    </button>
                </div>
            </div>
        );
    }

    if (hasKey) {
        return (
            <div className="w-full mt-8 bg-emerald-50 rounded-lg p-4 flex flex-col items-center justify-center gap-2 border border-emerald-100">
                <div className="flex items-center gap-2">
                    <ShieldCheck className="w-5 h-5 text-emerald-600" />
                    <span className="text-xs font-medium text-emerald-800">
                        단말기 암호화 키 등록 완료
                    </span>
                </div>
                <button 
                    onClick={() => {
                        setIsRegistering(true);
                        setKeyInput('');
                    }}
                    className="mt-2 text-xs px-3 py-1 bg-emerald-100 hover:bg-emerald-200 text-emerald-700 rounded transition-colors"
                >
                    키 재등록
                </button>
            </div>
        );
    }

    return (
        <div className="w-full mt-8 bg-orange-50 rounded-lg p-4 flex flex-col items-center justify-center gap-2 border border-orange-100">
            <div className="flex items-center gap-2">
                <ShieldAlert className="w-5 h-5 text-orange-600" />
                <span className="text-xs font-medium text-orange-800">
                    단말기 암호화 키 미등록
                </span>
            </div>
            <button 
                onClick={() => setIsRegistering(true)}
                className="mt-2 w-full py-2 bg-orange-100 hover:bg-orange-200 text-orange-700 text-xs font-semibold rounded transition-colors"
            >
                단말기 키 등록하기
            </button>
        </div>
    );
};

export default SecurityStatus;