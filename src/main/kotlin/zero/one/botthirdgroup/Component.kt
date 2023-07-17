package zero.one.botthirdgroup

import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

@Component
class DataLoader(
    private val languageRepository: LanguageRepository
) : CommandLineRunner {


    override fun run(vararg args: String?) {


        if (languageRepository.findAll().isEmpty()) {
            languageRepository.save(Language(LanguageName.UZ))
            languageRepository.save(Language(LanguageName.RU))
            languageRepository.save(Language(LanguageName.ENG))
        }

    }

}