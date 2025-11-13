from flask import Flask, render_template


app = Flask(__name__)


# Route for the home page (login)
@app.route('/')
def login():
    return render_template('login.html')


# Route for the register page
@app.route('/register')
def register():
    return render_template('register.html')


if __name__ == '__main__':
    app.run(debug=True)