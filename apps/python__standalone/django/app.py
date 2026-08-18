"""Django standalone service (single-file for benchmark parity).
Business behavior: GET /standalone returns "Greetings!"
"""
import os
import django
from django.conf import settings
from django.http import JsonResponse
from django.urls import path

settings.configure(
    DEBUG=False,
    ALLOWED_HOSTS=["*"],
    ROOT_URLCONF=__name__,
    SECRET_KEY="benchmark-secret",
    INSTALLED_APPS=[],
    MIDDLEWARE=[],
)
django.setup()


class StandaloneService:
    def return_message(self) -> str:
        return "Greetings!"


service = StandaloneService()


def greet(request):
    return JsonResponse({"message": service.return_message()})


def root(request):
    return JsonResponse({"message": service.return_message()})


urlpatterns = [
    path("standalone", greet),
    path("", root),
]

if __name__ == "__main__":
    from django.core.management import execute_from_command_line
    os.environ.setdefault("DJANGO_SETTINGS_MODULE", "__main__")
    execute_from_command_line(["manage.py", "runserver", "0.0.0.0:8080", "--noreload"])
