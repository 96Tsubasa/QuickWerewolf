
import { BrowserRouter, Routes, Route } from 'react-router-dom';

function App() {
  return (
    <BrowserRouter>
      <div className="app-container">
        <header>
          <h1 style={{ color: 'var(--accent-primary)', fontSize: '2.5rem' }}>QuickWerewolf</h1>
          <p style={{ color: 'var(--text-secondary)' }}>A mobile-first social deduction game</p>
        </header>
        <main className="glass-panel" style={{ marginTop: '2rem' }}>
          <Routes>
            <Route path="/" element={
              <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', alignItems: 'center' }}>
                <h2>Welcome</h2>
                <div style={{ display: 'flex', gap: '1rem' }}>
                  <button>Create Room</button>
                  <button style={{ background: 'var(--bg-tertiary)' }}>Join Room</button>
                </div>
              </div>
            } />
            <Route path="/room/:id" element={<div><h2>Room Loading...</h2></div>} />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  );
}

export default App;
