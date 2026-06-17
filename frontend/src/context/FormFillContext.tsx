import React, { createContext, useContext, useState, ReactNode } from 'react';

export interface FormFillState {
    reportId: number | null;
    parameters: Record<string, string>;
    timestamp: number;
}

interface FormFillContextType {
    formFillState: FormFillState | null;
    setFormFillState: (state: FormFillState | null) => void;
}

const FormFillContext = createContext<FormFillContextType | undefined>(undefined);

export const FormFillProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
    const [formFillState, setFormFillState] = useState<FormFillState | null>(null);

    return (
        <FormFillContext.Provider value={{ formFillState, setFormFillState }}>
            {children}
        </FormFillContext.Provider>
    );
};

export const useFormFill = () => {
    const context = useContext(FormFillContext);
    if (context === undefined) {
        throw new Error('useFormFill must be used within a FormFillProvider');
    }
    return context;
};
