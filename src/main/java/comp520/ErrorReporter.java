package comp520;

import java.util.ArrayList;
import java.util.List;
public class ErrorReporter {
    private List<String> _errorQueue;

    public ErrorReporter() {
        this._errorQueue = new ArrayList<>();
    }

    public boolean hasErrors() {
        return !this._errorQueue.isEmpty();
    }

    public void reportError(String ...error) {
        StringBuilder sb = new StringBuilder();

        for (String s : error) {
            sb.append(s);
        }

        _errorQueue.add(sb.toString());
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (this.hasErrors()) {
            for (String s : _errorQueue) {
                sb.append(s);
                sb.append("\n");
            }
        }
        return sb.toString();
    }
}
