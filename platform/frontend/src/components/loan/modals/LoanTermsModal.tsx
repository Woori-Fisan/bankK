import React, { useRef, useEffect, useState } from 'react';
import { FileText, X } from 'lucide-react';
import { Button } from '../../common/Button';
import type { ReviewDocument, ContractDocument } from '../../../api/loanApi';

interface LoanTermsModalProps {
    isOpen: boolean;
    onClose: () => void;
    activeDoc: ReviewDocument | ContractDocument | null;
    onAgree: () => void;
}

const LoanTermsModal: React.FC<LoanTermsModalProps> = ({
    isOpen,
    onClose,
    activeDoc,
    onAgree,
}) => {
    const [hasScrolledToBottom, setHasScrolledToBottom] = useState(false);
    const iframeRef = useRef<HTMLIFrameElement>(null);

    useEffect(() => {
        if (!isOpen) {
            setHasScrolledToBottom(false);
            return;
        }

        const handler = (e: MessageEvent) => {
            if (e.data === 'terms-scrolled-to-bottom') {
                setHasScrolledToBottom(true);
            }
        };
        window.addEventListener('message', handler);
        return () => window.removeEventListener('message', handler);
    }, [isOpen]);

    useEffect(() => {
        // 문서가 바뀌거나 모달이 새로 열릴 때마다 상태 초기화
        setHasScrolledToBottom(false);
    }, [activeDoc?.documentType, isOpen]);

    const handleClose = () => {
        setHasScrolledToBottom(false);
        onClose();
    };

    const buildTermsSrcDoc = (content: string | undefined): string => {
        const body = content ?? '<p style="padding:16px;font-family:sans-serif;color:#555">내용을 불러올 수 없습니다.</p>';
        const origin = window.location.origin;
        return `<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<style>
  html,body{margin:0;padding:24px;font-family:-apple-system,BlinkMacSystemFont,'Malgun Gothic','맑은 고딕',sans-serif;font-size:17px;line-height:1.8;color:#222;overflow-x:hidden;}
  p,li,span,div,td,th,label{font-size:17px !important;line-height:1.8 !important;}
  h1{font-size:1.4em !important;margin-top:0;} h2{font-size:1.25em !important;} h3{font-size:1.15em !important;}
  table{width:100%;border-collapse:collapse;font-size:17px !important;margin:16px 0;}
  td,th{padding:10px;border:1px solid #eee;}
</style>
</head>
<body>
<div id="content-wrapper">${body}</div>
<script>
(function(){
  var sent = false;
  function check(){
    if(sent) return;
    var wrapper = document.getElementById('content-wrapper');
    if(!wrapper) return;
    
    var scrollPos = Math.round(window.scrollY + window.innerHeight);
    var totalHeight = Math.max(
      document.documentElement.scrollHeight,
      document.body.scrollHeight,
      wrapper.offsetHeight
    );
    
    if(scrollPos >= totalHeight - 25){
      sent = true;
      window.parent.postMessage('terms-scrolled-to-bottom','${origin}');
    }
  }
  window.addEventListener('scroll', check);
  window.addEventListener('load', function(){
    setTimeout(check, 100);
  });
  window.addEventListener('resize', check);
})();
</` + `script>
</body>
</html>`;
    };

    if (!isOpen || !activeDoc) return null;

    return (
        <div className="fixed inset-0 z-[100] flex items-center justify-center bg-slate-900/60 backdrop-blur-sm p-4 animate-in fade-in duration-300">
            <div className="bg-white rounded-[32px] shadow-2xl w-full max-w-4xl overflow-hidden flex flex-col h-[85vh] animate-in zoom-in-95 duration-300">
                <div className="bg-slate-900 text-white px-10 py-8 flex items-center justify-between shrink-0">
                    <div className="flex items-center gap-4">
                        <div className="w-12 h-12 bg-white/10 rounded-2xl flex items-center justify-center">
                            <FileText className="w-6 h-6 text-slate-400" />
                        </div>
                        <h3 className="text-xl font-black">{activeDoc.documentName}</h3>
                    </div>
                    <button
                        type="button"
                        onClick={handleClose}
                        className="w-10 h-10 flex items-center justify-center rounded-xl hover:bg-white/10 transition-colors"
                    >
                        <X className="w-6 h-6" />
                    </button>
                </div>
                
                <div className="flex-1 min-h-0 bg-white">
                    <iframe
                        ref={iframeRef}
                        srcDoc={buildTermsSrcDoc(activeDoc.documentContent)}
                        className="w-full h-full border-0 block"
                        sandbox="allow-scripts"
                        title={activeDoc.documentName}
                    />
                </div>

                <div className="px-10 py-8 border-t border-slate-50 bg-slate-50/30 flex flex-col gap-6 shrink-0">
                    {!hasScrolledToBottom && (
                        <p className="text-sm text-rose-500 text-center font-black animate-bounce">
                            * 약관을 끝까지 읽어주셔야 동의가 가능합니다.
                        </p>
                    )}
                    <div className="flex justify-end gap-4">
                        <Button
                            onClick={handleClose}
                            variant="secondary"
                            className="px-8 h-14 rounded-2xl bg-white border border-slate-200 text-slate-600 font-black"
                        >
                            닫기
                        </Button>
                        <Button
                            onClick={onAgree}
                            disabled={!hasScrolledToBottom}
                            className={`px-10 h-14 rounded-2xl font-black transition-all ${
                                hasScrolledToBottom
                                    ? 'bg-slate-900 text-white hover:bg-slate-800 shadow-lg'
                                    : 'bg-slate-200 text-slate-400 cursor-not-allowed'
                            }`}
                        >
                            동의하고 확인
                        </Button>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default LoanTermsModal;
