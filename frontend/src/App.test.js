import { render, screen } from '@testing-library/react';
import App from './App';

test('renders the LegacyMind graph controls', () => {
  render(<App />);
  expect(screen.getByRole('button', { name: /analyze graph/i })).toBeInTheDocument();
  expect(screen.getByText(/nodes: 0/i)).toBeInTheDocument();
});
