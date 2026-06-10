import { BrowserRouter } from 'react-router-dom';
import Router from './routes/Router';
import { useAuthInit } from './hooks/useAuthInit';

function App() {
    const { isInitializing } = useAuthInit();

    if (isInitializing) return null;

    return (
        <BrowserRouter>
            <Router />
        </BrowserRouter>
    );
}

export default App;
