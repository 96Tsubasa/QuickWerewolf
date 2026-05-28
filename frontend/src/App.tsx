import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { Home } from './pages/Home';
import { Lobby } from './pages/Lobby';
import './App.css'; // Optional if it has styles you want to keep, but mostly index.css is enough

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/room/:roomCode" element={<Lobby />} />
      </Routes>
    </Router>
  );
}

export default App;
