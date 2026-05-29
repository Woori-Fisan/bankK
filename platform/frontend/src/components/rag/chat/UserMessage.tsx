import React from 'react';

interface UserMessageProps {
    content: string;
}

const UserMessage: React.FC<UserMessageProps> = ({ content }) => {
    return (
        <div className="flex flex-row-reverse gap-2">
            <div className="bg-emerald-600 p-3 rounded-2xl rounded-tr-none shadow-sm border border-emerald-500 max-w-[80%] text-white">
                <p className="text-sm whitespace-pre-wrap">{content}</p>
            </div>
        </div>
    );
};

export default UserMessage;
